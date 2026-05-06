package br.com.uebiescola.core.application.usecase;

import br.com.uebiescola.core.domain.model.School;
import br.com.uebiescola.core.domain.model.User;
import br.com.uebiescola.core.domain.model.enums.UserRole;
import br.com.uebiescola.core.domain.repository.SchoolRepository;
import br.com.uebiescola.core.domain.repository.UserRepository;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolEntity;
import br.com.uebiescola.core.infrastructure.persistence.entity.UserEntity;
import br.com.uebiescola.core.infrastructure.persistence.mapper.SchoolPersistenceMapper;
import br.com.uebiescola.core.presentation.dto.SchoolRequest; // Importe o DTO correto
import br.com.uebiescola.core.presentation.dto.TechnicalRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateSchoolUseCase {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public School execute(School school, TechnicalRequest tech) {

        if (school.getExternalId() == null) {
            school.setExternalId(UUID.randomUUID());
        }
        if (school.getUuid() == null) {
            school.setUuid(UUID.randomUUID());
        }

        // 1. Prepara o Admin no domínio
        User adminUser = User.builder()
                .name(tech.adminName())
                .email(tech.adminEmail())
                .cpf(tech.adminCpf())
                .role(UserRole.ROLE_ADMIN)
                .externalId(UUID.randomUUID())
                .build();
        school.setAdminUser(adminUser);

        String encodedPassword = passwordEncoder.encode(tech.adminPassword());

        // 2. Salva usando a senha criptografada
        // O savedSchool já volta com os IDs do banco e o Admin vinculado
        School savedSchool = schoolRepository.saveWithAdminPassword(school, encodedPassword);
        // 3. Agora que a escola tem ID, vinculamos o schoolId no usuário
        if (savedSchool.getAdminUser() != null) {
            User admin = savedSchool.getAdminUser();
            admin.setSchoolId(savedSchool.getId());

            // Salvamos através do userRepository (Domínio -> Adapter -> Jpa)
            userRepository.save(admin);
        }

        return savedSchool;
    }
}