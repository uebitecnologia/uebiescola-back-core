package br.com.uebiescola.core.application.usecase;

import br.com.uebiescola.core.domain.model.School;
import br.com.uebiescola.core.domain.model.SchoolAddress;
import br.com.uebiescola.core.domain.model.SchoolContract;
import br.com.uebiescola.core.domain.model.User;
import br.com.uebiescola.core.domain.model.enums.UserRole;
import br.com.uebiescola.core.domain.repository.SchoolRepository;
import br.com.uebiescola.core.domain.repository.UserRepository;
import br.com.uebiescola.core.infrastructure.client.IamInternalClient;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolRepository;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaUserRepository;
import br.com.uebiescola.core.presentation.dto.NfacilSignupRequest;
import br.com.uebiescola.core.presentation.dto.NfacilSignupResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Signup self-service do produto NF Facil (NFS-e automatica pra escola PJ).
 *
 * Diferencas do SelfServiceRegistrationUseCase:
 * - Sem senha — login via magic link (token 7d gerado pelo iam-service)
 * - Plano base "NF Facil" (escola so usa modulo FINANCE)
 * - Cobranca so dispara apos homologacao da prefeitura
 * - Aceita apenas PJ (CNPJ obrigatorio)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NfacilSignupUseCase {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final JpaSchoolRepository jpaSchoolRepository;
    private final JpaUserRepository jpaUserRepository;
    private final IamInternalClient iamInternalClient;

    @Value("${uebi.internal-token}")
    private String internalToken;

    @Value("${app.mail.frontend-url:https://admin.uebiescola.com.br}")
    private String frontendUrl;

    @Transactional
    public NfacilSignupResponse execute(NfacilSignupRequest request) {
        String cnpjDigits = request.cnpj().replaceAll("\\D", "");

        if (jpaSchoolRepository.existsByCnpj(cnpjDigits)) {
            throw new IllegalArgumentException(
                    "CNPJ ja cadastrado. Use o link de acesso enviado por email ou fale com nosso time.");
        }
        if (jpaUserRepository.existsByEmail(request.adminEmail())) {
            throw new IllegalArgumentException("Este email ja esta cadastrado em outra escola.");
        }

        String subdomain = generateUniqueSubdomain(request.schoolName());

        SchoolAddress address = SchoolAddress.builder()
                .city(request.city())
                .state(request.state().toUpperCase())
                .phone(request.adminPhone())
                .mobile(request.adminPhone())
                .build();

        SchoolContract contract = SchoolContract.builder()
                .planBase("NF Facil")
                .activeModules(List.of("FINANCE"))
                .monthlyValue(new BigDecimal("97.00"))
                .setupValue(BigDecimal.ZERO)
                .startDate(LocalDate.now())
                .expirationDay(LocalDate.now().getDayOfMonth())
                .billingCycle("MONTHLY")
                .billingType("PIX")
                .build();

        School school = School.builder()
                .externalId(UUID.randomUUID())
                .name(request.fantasyName() != null && !request.fantasyName().isBlank()
                        ? request.fantasyName() : request.schoolName())
                .legalName(request.schoolName())
                .cnpj(cnpjDigits)
                .subdomain(subdomain)
                .active(true)
                .address(address)
                .contract(contract)
                .build();

        User adminUser = User.builder()
                .name(request.adminName())
                .email(request.adminEmail())
                .role(UserRole.ROLE_ADMIN)
                .externalId(UUID.randomUUID())
                .build();
        school.setAdminUser(adminUser);

        // Senha aleatoria descartavel — escola loga via magic link
        String placeholderPassword = UUID.randomUUID().toString();
        School saved = schoolRepository.saveWithAdminPassword(school, placeholderPassword);

        if (saved.getAdminUser() != null) {
            User admin = saved.getAdminUser();
            admin.setSchoolId(saved.getId());
            userRepository.save(admin);
        }

        log.info("[NFACIL-SIGNUP] escola criada: id={} cnpj={} slug={} email={}",
                saved.getId(), cnpjDigits, subdomain, request.adminEmail());

        try {
            iamInternalClient.inviteUser(
                    new IamInternalClient.InviteUserRequest(
                            request.adminName(), null, request.adminEmail(),
                            UserRole.ROLE_ADMIN.name(), saved.getId(), null),
                    internalToken);
            log.info("[NFACIL-SIGNUP] magic link enviado pra {}", request.adminEmail());
        } catch (Exception e) {
            log.error("[NFACIL-SIGNUP] falha ao enviar magic link pra {}: {}",
                    request.adminEmail(), e.getMessage());
            // Nao quebra o signup — escola pode pedir reenvio via /entrar > esqueci minha senha
        }

        String loginUrl = "https://" + subdomain + ".uebiescola.com.br";

        return new NfacilSignupResponse(
                saved.getExternalId(),
                subdomain,
                loginUrl,
                "Conta criada. Confira o email com seu link de acesso."
        );
    }

    private String generateUniqueSubdomain(String schoolName) {
        String base = schoolName.toLowerCase()
                .replaceAll("[áàãâä]", "a")
                .replaceAll("[éèêë]", "e")
                .replaceAll("[íìîï]", "i")
                .replaceAll("[óòõôö]", "o")
                .replaceAll("[úùûü]", "u")
                .replaceAll("ç", "c")
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        if (base.length() > 30) base = base.substring(0, 30);
        if (base.isEmpty()) base = "escola";

        String candidate = base;
        int counter = 1;
        while (jpaSchoolRepository.findBySubdomain(candidate).isPresent()) {
            candidate = base + "-" + counter;
            counter++;
        }
        return candidate;
    }
}
