package br.com.uebiescola.core.application.usecase;

import br.com.uebiescola.core.domain.model.School;
import br.com.uebiescola.core.domain.model.SchoolContract;
import br.com.uebiescola.core.domain.model.User;
import br.com.uebiescola.core.domain.model.enums.UserRole;
import br.com.uebiescola.core.domain.repository.SchoolRepository;
import br.com.uebiescola.core.domain.repository.UserRepository;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolRepository;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaUserRepository;
import br.com.uebiescola.core.presentation.dto.SchoolRegistrationRequest;
import br.com.uebiescola.core.presentation.dto.SchoolRegistrationResponse;
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
public class RegisterSchoolUseCase {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final JpaUserRepository jpaUserRepository;
    private final JpaSchoolRepository jpaSchoolRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SchoolRegistrationResponse execute(SchoolRegistrationRequest request) {
        // Validações
        if (jpaUserRepository.existsByEmail(request.adminEmail())) {
            throw new IllegalArgumentException("Este email já está cadastrado.");
        }
        if (jpaUserRepository.existsByCpf(request.adminCpf())) {
            throw new IllegalArgumentException("Este CPF já está cadastrado.");
        }

        if (jpaSchoolRepository.existsByCnpj(request.cnpj())) {
            throw new IllegalArgumentException("Este CNPJ já está cadastrado.");
        }

        // Gerar subdomain a partir do nome se não informado
        String subdomain = request.subdomain();
        if (subdomain == null || subdomain.isBlank()) {
            subdomain = generateSubdomain(request.schoolName());
        }

        // Verificar subdomain único
        if (jpaSchoolRepository.findBySubdomain(subdomain).isPresent()) {
            throw new IllegalArgumentException("Este subdomínio já está em uso.");
        }

        // Criar escola
        School school = new School();
        school.setExternalId(UUID.randomUUID());
        school.setName(request.schoolName());
        school.setLegalName(request.schoolName());
        school.setCnpj(request.cnpj());
        school.setSubdomain(subdomain);
        school.setActive(true);

        // Contrato trial (30 dias grátis)
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

        // Salvar
        School savedSchool = schoolRepository.saveWithAdminPassword(school, encodedPassword);

        // Vincular schoolId ao admin
        if (savedSchool.getAdminUser() != null) {
            User admin = savedSchool.getAdminUser();
            admin.setSchoolId(savedSchool.getId());
            userRepository.save(admin);
        }

        log.info("Nova escola registrada: {} ({})", savedSchool.getName(), savedSchool.getSubdomain());

        return new SchoolRegistrationResponse(
                savedSchool.getId(),
                savedSchool.getExternalId(),
                savedSchool.getName(),
                savedSchool.getSubdomain(),
                request.adminEmail(),
                "Escola registrada com sucesso! Faça login com seu email e senha."
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
