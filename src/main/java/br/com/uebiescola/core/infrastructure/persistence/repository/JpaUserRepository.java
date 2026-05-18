package br.com.uebiescola.core.infrastructure.persistence.repository;

import br.com.uebiescola.core.domain.model.enums.UserRole;
import br.com.uebiescola.core.infrastructure.persistence.entity.UserEntity;
import org.antlr.v4.runtime.misc.MultiMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByExternalId(UUID externalId);
    List<UserEntity> findAllBySchoolId(Long schoolId);
    Optional<UserEntity> findFirstBySchoolIdAndRole(Long schoolId, UserRole role);
    List<UserEntity> findAllBySchoolIdIsNull();
    boolean existsByEmail(String email);
    boolean existsByCpf(String cpf);

    // Validações ESCOPADAS por escola — alinhadas com o índice parcial unique
    // (V18) que permite mesmo CPF/email em escolas diferentes.
    boolean existsByEmailAndSchoolId(String email, Long schoolId);
    boolean existsByCpfAndSchoolId(String cpf, Long schoolId);

    // Validação CROSS-SCHOOL pra ROLE_CEO — CEO não tem schoolId fixo e deve ser
    // único globalmente. O @SQLRestriction da entity já filtra deletados.
    boolean existsByEmailAndRole(String email, br.com.uebiescola.core.domain.model.enums.UserRole role);
    boolean existsByCpfAndRole(String cpf, br.com.uebiescola.core.domain.model.enums.UserRole role);
}