package br.com.uebiescola.core.infrastructure.config;

import br.com.uebiescola.core.domain.model.UserRole;
import br.com.uebiescola.core.infrastructure.persistence.entity.UserEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminUserSeed implements CommandLineRunner {

    private final JpaUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@uebiescola.com.br").isEmpty()) {
            UserEntity admin = UserEntity.builder()
                    .externalId(UUID.randomUUID())
                    .name("CEO Uebi")
                    .email("admin@uebiescola.com.br")
                    .password(passwordEncoder.encode("admin123")) // Senha para o Postman
                    .role(UserRole.ROLE_CEO)
                    .schoolId(null) // CEO não pertence a uma escola específica
                    .build();

            userRepository.save(admin);
            System.out.println("✅ Usuário CEO criado com sucesso: admin@uebiescola.com.br / admin123");
        }
    }
}