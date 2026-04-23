package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.service.EmailService;
import br.com.uebiescola.core.application.usecase.RegisterSchoolUseCase;
import br.com.uebiescola.core.application.usecase.SelfServiceRegistrationUseCase;
import br.com.uebiescola.core.infrastructure.persistence.entity.UserEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaUserRepository;
import br.com.uebiescola.core.presentation.dto.SchoolRegistrationRequest;
import br.com.uebiescola.core.presentation.dto.SchoolRegistrationResponse;
import br.com.uebiescola.core.presentation.dto.SelfServiceRegistrationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/register")
@RequiredArgsConstructor
@Slf4j
public class RegistrationController {

    private final RegisterSchoolUseCase registerSchoolUseCase;
    private final SelfServiceRegistrationUseCase selfServiceRegistrationUseCase;
    private final JpaUserRepository jpaUserRepository;
    private final EmailService emailService;
    private static final SecureRandom RANDOM = new SecureRandom();

    @PostMapping
    public ResponseEntity<?> registerSchool(@RequestBody @Valid SchoolRegistrationRequest request) {
        try {
            SchoolRegistrationResponse response = registerSchoolUseCase.execute(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            log.warn("Violação de constraint no registro: {}", e.getMostSpecificCause().getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Dados duplicados. Email, CPF ou CNPJ já cadastrado."));
        }
    }

    @PostMapping("/self-service")
    public ResponseEntity<?> selfServiceRegistration(@RequestBody @Valid SelfServiceRegistrationRequest request) {
        try {
            SchoolRegistrationResponse response = selfServiceRegistrationUseCase.execute(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            log.warn("Violação de constraint no self-service: {}", e.getMostSpecificCause().getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Dados duplicados. Email ou CPF já cadastrado."));
        }
    }

    /**
     * Confirma o codigo de 6 digitos enviado por email apos o cadastro self-service.
     * Corpo: { "email": "...", "code": "123456" }
     */
    @PostMapping("/verify-email")
    @Transactional
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        if (email == null || code == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Informe email e codigo."));
        }
        UserEntity user = jpaUserRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Email nao encontrado."));
        }
        if (user.getEmailVerifiedAt() != null) {
            return ResponseEntity.ok(Map.of("message", "Email ja verificado.", "verified", true));
        }
        if (user.getEmailVerificationCode() == null
                || user.getEmailVerificationExpiresAt() == null
                || LocalDateTime.now().isAfter(user.getEmailVerificationExpiresAt())) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("message", "Codigo expirado. Solicite um novo."));
        }
        if (!code.equals(user.getEmailVerificationCode())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Codigo invalido."));
        }
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setEmailVerificationCode(null);
        user.setEmailVerificationExpiresAt(null);
        jpaUserRepository.save(user);
        log.info("Email verificado: {}", email);
        return ResponseEntity.ok(Map.of("message", "Email verificado com sucesso.", "verified", true));
    }

    /**
     * Reenvia o codigo de verificacao por email (rate-limit informal: 1 por minuto).
     */
    @PostMapping("/resend-code")
    @Transactional
    public ResponseEntity<?> resendCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Informe o email."));
        }
        UserEntity user = jpaUserRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Email nao encontrado."));
        }
        if (user.getEmailVerifiedAt() != null) {
            return ResponseEntity.ok(Map.of("message", "Email ja verificado."));
        }
        // Gera codigo novo
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        user.setEmailVerificationCode(code);
        user.setEmailVerificationExpiresAt(LocalDateTime.now().plusMinutes(30));
        jpaUserRepository.save(user);
        try {
            emailService.sendEmailVerificationCode(email, user.getName(), code);
        } catch (Exception e) {
            log.error("Falha ao reenviar codigo para {}: {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "Nao foi possivel enviar o email. Tente de novo em alguns minutos."));
        }
        return ResponseEntity.ok(Map.of("message", "Codigo reenviado."));
    }
}
