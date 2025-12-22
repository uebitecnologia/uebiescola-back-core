package br.com.uebiescola.core.application.usecase;

import br.com.uebiescola.core.domain.model.School;
import br.com.uebiescola.core.domain.model.User;
import br.com.uebiescola.core.domain.model.UserRole;
import br.com.uebiescola.core.domain.repository.SchoolRepository;
import br.com.uebiescola.core.domain.repository.UserRepository;
import br.com.uebiescola.core.infrastructure.persistence.entity.UserEntity;
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
        // 1. Regra de Negócio: Validar se o subdomínio já existe
        schoolRepository.findBySubdomain(school.getSubdomain())
                .ifPresent(s -> {
                    throw new RuntimeException("Subdomínio já está em uso por outra escola.");
                });

        // 2. Salvar a Escola e pegar o ID gerado
        school.setExternalId(UUID.randomUUID());
        school.setActive(true);
        School savedSchool = schoolRepository.save(school);

        // 3. Criar o primeiro usuário ADMIN daquela escola
        User admin = User.builder()
                .externalId(UUID.randomUUID())
                .name(tech.adminName())
                .email(tech.adminEmail())
                .password(passwordEncoder.encode(tech.adminPassword())) // Criptografia!
                .role(UserRole.ROLE_ADMIN)
                .schoolId(savedSchool.getId()) // O vínculo de Multi-tenancy
                .build();

        userRepository.save(admin);

        return savedSchool;
    }
}