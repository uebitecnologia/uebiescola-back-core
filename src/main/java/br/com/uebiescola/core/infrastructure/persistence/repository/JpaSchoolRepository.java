package br.com.uebiescola.core.infrastructure.persistence.repository;

import br.com.uebiescola.core.domain.projection.GrowthStatsProjection;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface JpaSchoolRepository extends JpaRepository<SchoolEntity, Long> {
    Optional<SchoolEntity> findBySubdomain(String subdomain);

    Optional<SchoolEntity> findByUuid(UUID uuid);

    boolean existsByCnpj(String cnpj);

    boolean existsByCpf(String cpf);

    Optional<SchoolEntity> findByReferralCode(String referralCode);

    boolean existsByReferralCode(String referralCode);

    @Modifying
    @Query("UPDATE SchoolEntity s SET s.active = :status WHERE s.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") Boolean status);

    /**
     * UPDATE cirurgico dos campos de config da Loja. Usa native SQL pra evitar
     * regeneracao da entity inteira via mapper — o mapper poderia sobrescrever
     * campos que nao vem no PUT (adminUser, address, etc). Cada param eh
     * opcional: se null, mantem o valor atual.
     */
    @Modifying
    @Query(value = "UPDATE schools SET " +
            "marketplace_enabled = COALESCE(CAST(:enabled AS boolean), marketplace_enabled), " +
            "marketplace_commission_percent = COALESCE(CAST(:pct AS numeric), marketplace_commission_percent), " +
            "marketplace_commission_cap = CASE WHEN :capChanged THEN CAST(:cap AS numeric) ELSE marketplace_commission_cap END " +
            "WHERE id = :id",
            nativeQuery = true)
    int updateMarketplaceConfig(@Param("id") Long id,
                                @Param("enabled") Boolean enabled,
                                @Param("pct") BigDecimal pct,
                                @Param("capChanged") boolean capChanged,
                                @Param("cap") BigDecimal cap);

    @Query("SELECT s.contract.planBase, COUNT(s) FROM SchoolEntity s GROUP BY s.contract.planBase")
    List<Object[]> countSchoolsByPlan();

    @Query(value = "SELECT to_char(created_at, 'Mon/YY') as month, count(*) as total " +
            "FROM schools WHERE deleted_at IS NULL GROUP BY month ORDER BY min(created_at)", nativeQuery = true)
    List<GrowthStatsProjection> getGrowthStats();
}