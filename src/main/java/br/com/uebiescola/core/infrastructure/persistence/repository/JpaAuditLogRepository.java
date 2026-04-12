package br.com.uebiescola.core.infrastructure.persistence.repository;

import br.com.uebiescola.core.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface JpaAuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    Page<AuditLogEntity> findBySchoolIdOrderByCreatedAtDesc(Long schoolId, Pageable pageable);

    @Query(value = "SELECT * FROM audit_logs a WHERE " +
            "(:schoolId IS NULL OR a.school_id = :schoolId) AND " +
            "(CAST(:action AS VARCHAR) IS NULL OR a.action = :action) AND " +
            "(CAST(:from AS TIMESTAMP) IS NULL OR a.created_at >= :from) AND " +
            "(CAST(:to AS TIMESTAMP) IS NULL OR a.created_at <= :to) " +
            "ORDER BY a.created_at DESC",
            countQuery = "SELECT COUNT(*) FROM audit_logs a WHERE " +
            "(:schoolId IS NULL OR a.school_id = :schoolId) AND " +
            "(CAST(:action AS VARCHAR) IS NULL OR a.action = :action) AND " +
            "(CAST(:from AS TIMESTAMP) IS NULL OR a.created_at >= :from) AND " +
            "(CAST(:to AS TIMESTAMP) IS NULL OR a.created_at <= :to)",
            nativeQuery = true)
    Page<AuditLogEntity> findAllWithFilters(
            @Param("schoolId") Long schoolId,
            @Param("action") String action,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
