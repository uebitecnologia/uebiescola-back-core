package br.com.uebiescola.core.application.usecase;

import br.com.uebiescola.core.domain.model.School;
import br.com.uebiescola.core.domain.model.SchoolContract;
import br.com.uebiescola.core.domain.model.User;
import br.com.uebiescola.core.domain.model.enums.UserRole;
import br.com.uebiescola.core.domain.repository.SchoolRepository;
import br.com.uebiescola.core.domain.repository.UserRepository;
import br.com.uebiescola.core.infrastructure.client.PlansSubscriptionClient;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolRepository;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaUserRepository;
import br.com.uebiescola.core.presentation.dto.SchoolRegistrationResponse;
import br.com.uebiescola.core.presentation.dto.SelfServiceRegistrationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SelfServiceRegistrationUseCase {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final JpaUserRepository jpaUserRepository;
    private final JpaSchoolRepository jpaSchoolRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlansSubscriptionClient plansSubscriptionClient;

    @Transactional
    public SchoolRegistrationResponse execute(SelfServiceRegistrationRequest request) {
        // Validações de unicidade
        if (jpaUserRepository.existsByEmail(request.adminEmail())) {
            throw new IllegalArgumentException("Este email já está cadastrado.");
        }
        if (jpaUserRepository.existsByCpf(request.adminCpf())) {
            throw new IllegalArgumentException("Este CPF já está cadastrado.");
        }

        // Gerar subdomain a partir do nome da escola
        String subdomain = generateSubdomain(request.schoolName());

        // Verificar subdomain único
        if (jpaSchoolRepository.findBySubdomain(subdomain).isPresent()) {
            throw new IllegalArgumentException("Este subdomínio já está em uso.");
        }

        // Criar escola
        School school = new School();
        school.setExternalId(UUID.randomUUID());
        school.setName(request.schoolName());
        school.setLegalName(request.schoolName());
        school.setSubdomain(subdomain);
        school.setActive(true);

        // Contrato trial local (14 dias)
        SchoolContract contract = new SchoolContract();
        contract.setPlanBase("Trial");
        contract.setMonthlyValue(BigDecimal.ZERO);
        contract.setSetupValue(BigDecimal.ZERO);
        contract.setStartDate(LocalDate.now());
        contract.setExpirationDay(LocalDate.now().getDayOfMonth());
        contract.setActiveModules(List.of(
                "ACADEMIC", "FINANCE", "ENROLLMENT", "COMMUNICATION"
        ));
        school.setContract(contract);

        // Criar admin user
        User adminUser = User.builder()
                .name(request.adminName())
                .email(request.adminEmail())
                .cpf(request.adminCpf())
                .role(UserRole.ROLE_ADMIN)
                .externalId(UUID.randomUUID())
                .build();
        school.setAdminUser(adminUser);

        String encodedPassword = passwordEncoder.encode(request.adminPassword());

        // Salvar escola e admin
        School savedSchool = schoolRepository.saveWithAdminPassword(school, encodedPassword);

        // Vincular schoolId ao admin
        if (savedSchool.getAdminUser() != null) {
            User admin = savedSchool.getAdminUser();
            admin.setSchoolId(savedSchool.getId());
            userRepository.save(admin);
        }

        log.info("Self-service: nova escola registrada: {} ({})", savedSchool.getName(), savedSchool.getSubdomain());

        // Criar assinatura TRIAL no Plans Service (7 dias — lei do CDC, direito de arrependimento)
        try {
            var trialResponse = plansSubscriptionClient.createTrialSubscription(
                    new PlansSubscriptionClient.TrialSubscriptionRequest(savedSchool.getId(), 7));
            log.info("Assinatura TRIAL criada no Plans Service para escola {}: plano {}",
                    savedSchool.getId(), trialResponse.planName());
        } catch (Exception e) {
            log.warn("Falha ao criar assinatura TRIAL no Plans Service para escola {}: {}",
                    savedSchool.getId(), e.getMessage());
            // Não interrompe o registro - a escola pode ficar sem subscription e será tratada como trial default
        }

        return new SchoolRegistrationResponse(
                savedSchool.getId(),
                savedSchool.getExternalId(),
                savedSchool.getName(),
                savedSchool.getSubdomain(),
                request.adminEmail(),
                "Escola registrada com sucesso! Você tem 14 dias de teste grátis. Faça login com seu email e senha."
        );
    }

    private String generateSubdomain(String schoolName) {
        String base = schoolName.toLowerCase()
                .replaceAll("[áàãâä]", "a")
                .replaceAll("[éèêë]", "e")
                .replaceAll("[íìîï]", "i")
                .replaceAll("[óòõôö]", "o")
                .replaceAll("[úùûü]", "u")
                .replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        if (base.length() > 30) base = base.substring(0, 30);

        // Se já existe, adiciona número
        String candidate = base;
        int counter = 1;
        while (jpaSchoolRepository.findBySubdomain(candidate).isPresent()) {
            candidate = base + "-" + counter;
            counter++;
        }
        return candidate;
    }
}
