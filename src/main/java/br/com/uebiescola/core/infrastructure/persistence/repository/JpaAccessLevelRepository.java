package br.com.uebiescola.core.infrastructure.persistence.repository;

import br.com.uebiescola.core.infrastructure.persistence.entity.AccessLevelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaAccessLevelRepository extends JpaRepository<AccessLevelEntity, Long> {
    List<AccessLevelEntity> findAllBySchoolIdOrderByNameAsc(Long schoolId);
    List<AccessLevelEntity> findAllBySchoolIdAndActiveTrue(Long schoolId);
    Optional<AccessLevelEntity> findByUuid(UUID uuid);
}
