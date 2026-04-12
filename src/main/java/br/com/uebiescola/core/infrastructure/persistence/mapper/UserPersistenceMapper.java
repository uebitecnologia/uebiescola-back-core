package br.com.uebiescola.core.infrastructure.persistence.mapper;

import br.com.uebiescola.core.domain.model.User;
import br.com.uebiescola.core.infrastructure.persistence.entity.UserEntity;

public class UserPersistenceMapper {
    public static User toDomain(UserEntity entity) {
        if (entity == null) return null;
        return User.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .name(entity.getName())
                .cpf(entity.getCpf())
                .email(entity.getEmail())
                .role(entity.getRole())
                .schoolId(entity.getSchoolId())
                .build();
    }
}