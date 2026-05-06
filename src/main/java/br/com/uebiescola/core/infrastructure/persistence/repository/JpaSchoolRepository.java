package br.com.uebiescola.core.infrastructure.persistence.repository;

import br.com.uebiescola.core.domain.projection.GrowthStatsProjection;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface JpaSchoolRepository extends JpaRepository<SchoolEntity, Long> {
    Optional<SchoolEntity> findBySubdomain(String subdomain);

    Optional<SchoolEntity> findByUuid(UUID uuid);

    boolean existsByCnpj(String cnpj);

    @Modifying
    @Query("UPDATE SchoolEntity s SET s.active = :status WHERE s.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") Boolean status);

    @Query("SELECT s.contract.planBase, COUNT(s) FROM SchoolEntity s GROUP BY s.contract.planBase")
    List<Object[]> countSchoolsByPlan();

    @Query(value = "SELECT to_char(created_at, 'Mon/YY') as month, count(*) as total " +
            "FROM schools WHERE deleted_at IS NULL GROUP BY month ORDER BY min(created_at)", nativeQuery = true)
    List<GrowthStatsProjection> getGrowthStats();
}