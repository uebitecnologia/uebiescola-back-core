package br.com.uebiescola.core.infrastructure.persistence.adapter;

import br.com.uebiescola.core.domain.model.User;
import br.com.uebiescola.core.domain.model.enums.UserRole;
import br.com.uebiescola.core.domain.repository.UserRepository;
import br.com.uebiescola.core.infrastructure.persistence.entity.UserEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;

    @Override
    public User save(User user) {
        UserEntity entity;

        if (user.getId() != null) {
            // 1. Se o usuário já tem ID, buscamos a entidade original do banco
            // Isso é vital para não perdermos a 'password' que já está gravada
            entity = jpaUserRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado para atualização"));
        } else {
            // 2. Se for um usuário novo, criamos uma nova instância
            entity = new UserEntity();
        }

        // 3. Atualizamos os campos que vêm do objeto de domínio
        entity.setExternalId(user.getExternalId());
        entity.setName(user.getName());
        entity.setCpf(user.getCpf());
        entity.setEmail(user.getEmail());
        entity.setRole(user.getRole());
        entity.setSchoolId(user.getSchoolId());
        if (user.getActive() != null) entity.setActive(user.getActive());

        // A propriedade 'entity.password' não é tocada aqui,
        // mantendo o valor que foi inserido na criação inicial da escola.

        UserEntity savedEntity = jpaUserRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public List<User> findAllBySchoolId(Long schoolId) {
        return jpaUserRepository.findAllBySchoolId(schoolId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<User> findFirstBySchoolIdAndRole(Long schoolId, UserRole role) {
        return jpaUserRepository.findFirstBySchoolIdAndRole(schoolId, role)
                .map(this::toDomain);
    }

    private User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return User.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .name(entity.getName())
                .cpf(entity.getCpf())
                .email(entity.getEmail())
                .role(entity.getRole())
                .schoolId(entity.getSchoolId())
                .active(entity.getActive())
                .build();
    }
}