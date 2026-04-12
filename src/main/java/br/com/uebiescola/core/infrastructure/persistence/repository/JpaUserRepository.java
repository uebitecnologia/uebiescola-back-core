package br.com.uebiescola.core.infrastructure.persistence.repository;

import br.com.uebiescola.core.domain.model.enums.UserRole;
import br.com.uebiescola.core.infrastructure.persistence.entity.UserEntity;
import org.antlr.v4.runtime.misc.MultiMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    List<UserEntity> findAllBySchoolId(Long schoolId);
    Optional<UserEntity> findFirstBySchoolIdAndRole(Long schoolId, UserRole role);
    List<UserEntity> findAllBySchoolIdIsNull();
    boolean existsByEmail(String email);
    boolean existsByCpf(String cpf);
}