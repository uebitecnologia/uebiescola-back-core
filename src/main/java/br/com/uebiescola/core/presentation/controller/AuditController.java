package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.infrastructure.persistence.entity.AuditLogEntity;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaAuditLogRepository;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolRepository;
import br.com.uebiescola.core.presentation.dto.AuditLogResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final JpaAuditLogRepository auditLogRepository;
    private final JpaSchoolRepository schoolRepository;

    /**
     * CEO-only: List all audit logs across all schools with optional filters.
     */
    @GetMapping
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<List<AuditLogResponseDTO>> getAllAuditLogs(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<AuditLogEntity> logs = auditLogRepository.findAllWithFilters(
                schoolId, action, from, to, PageRequest.of(page, size));

        // Pre-load school names for the results
        List<Long> schoolIds = logs.getContent().stream()
                .map(AuditLogEntity::getSchoolId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> schoolNames = schoolRepository.findAllById(schoolIds).stream()
                .collect(Collectors.toMap(SchoolEntity::getId, SchoolEntity::getName));

        List<AuditLogResponseDTO> result = logs.getContent().stream()
                .map(log -> new AuditLogResponseDTO(
                        log.getId(),
                        log.getSchoolId(),
                        schoolNames.getOrDefault(log.getSchoolId(), "Desconhecida"),
                        log.getUserEmail(),
                        log.getAction(),
                        log.getDetails(),
                        log.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Void> createAuditLog(@RequestBody Map<String, Object> request) {
        Long schoolId = request.get("schoolId") != null
                ? ((Number) request.get("schoolId")).longValue()
                : null;

        AuditLogEntity log = AuditLogEntity.builder()
                .schoolId(schoolId)
                .userEmail((String) request.get("userEmail"))
                .action((String) request.get("action"))
                .details((String) request.get("details"))
                .build();
        auditLogRepository.save(log);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
