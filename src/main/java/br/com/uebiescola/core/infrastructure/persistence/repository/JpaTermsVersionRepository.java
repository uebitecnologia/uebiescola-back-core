package br.com.uebiescola.core.infrastructure.persistence.repository;

import br.com.uebiescola.core.domain.model.enums.TermsType;
import br.com.uebiescola.core.infrastructure.persistence.entity.TermsVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaTermsVersionRepository extends JpaRepository<TermsVersionEntity, Long> {

    Optional<TermsVersionEntity> findFirstByTypeAndActiveTrueOrderByCreatedAtDesc(TermsType type);

    Optional<TermsVersionEntity> findByUuid(UUID uuid);

    List<TermsVersionEntity> findByTypeAndActiveTrue(TermsType type);

    @Modifying
    @Query("UPDATE TermsVersionEntity t SET t.active = false WHERE t.type = :type")
    void deactivateAllByType(@Param("type") TermsType type);
}
