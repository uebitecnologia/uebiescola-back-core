package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.usecase.RegisterSchoolUseCase;
import br.com.uebiescola.core.application.usecase.SelfServiceRegistrationUseCase;
import br.com.uebiescola.core.presentation.dto.SchoolRegistrationRequest;
import br.com.uebiescola.core.presentation.dto.SchoolRegistrationResponse;
import br.com.uebiescola.core.presentation.dto.SelfServiceRegistrationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/register")
@RequiredArgsConstructor
@Slf4j
public class RegistrationController {

    private final RegisterSchoolUseCase registerSchoolUseCase;
    private final SelfServiceRegistrationUseCase selfServiceRegistrationUseCase;

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
}
