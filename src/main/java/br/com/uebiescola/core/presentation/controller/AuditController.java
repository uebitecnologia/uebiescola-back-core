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
    /** D-3 AUDITORIAADMINPLATAFORMA 03/09/2026: teto no size pra evitar
     *  dump completo em uma requisicao (Chrome fez ?size=2000 e recebeu
     *  a base inteira de auditoria). */
    private static final int MAX_PAGE_SIZE = 100;

    @GetMapping
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<Map<String, Object>> getAllAuditLogs(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        int effectiveSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<AuditLogEntity> logs = auditLogRepository.findAllWithFilters(
                schoolId, action, from, to, PageRequest.of(page, effectiveSize));

        // Pre-load school names for the results
        List<Long> schoolIds = logs.getContent().stream()
                .map(AuditLogEntity::getSchoolId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> schoolNames = schoolRepository.findAllById(schoolIds).stream()
                .collect(Collectors.toMap(SchoolEntity::getId, SchoolEntity::getName));

        List<AuditLogResponseDTO> content = logs.getContent().stream()
                .map(log -> new AuditLogResponseDTO(
                        log.getSchoolId(),
                        // REVALIDACAO 06/09/2026: schoolId null (leitura CEO
                        // cross-tenant) e legitimo — mostra "(global)" em vez
                        // de "Desconhecida", que era enganoso.
                        log.getSchoolId() == null
                                ? "(global)"
                                : schoolNames.getOrDefault(log.getSchoolId(), "Desconhecida"),
                        log.getUserEmail(),
                        log.getAction(),
                        log.getDetails(),
                        log.getCreatedAt(),
                        log.getIpAddress()
                ))
                .toList();

        // D-3 AUDITORIAADMINPLATAFORMA: envelope com totalElements, que a
        // resposta anterior nao trazia — front nao conseguia dizer quantos ha.
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("content", content);
        body.put("totalElements", logs.getTotalElements());
        body.put("totalPages", logs.getTotalPages());
        body.put("page", logs.getNumber());
        body.put("size", logs.getSize());
        body.put("maxSize", MAX_PAGE_SIZE);
        return ResponseEntity.ok(body);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> createAuditLog(
            @RequestBody Map<String, Object> request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
                br.com.uebiescola.core.infrastructure.security.AuthenticatedUser user) {

        // schoolId tem precedencia do body; se ausente, usa o do usuario autenticado.
        // REVALIDACAO-2 06/09/2026: cast seguro pra evitar NPE em Number.longValue()
        // quando body traz schoolId=null explicito (log CEO cross-tenant).
        Long schoolId = null;
        Object rawSchoolId = request.get("schoolId");
        if (rawSchoolId instanceof Number n) {
            schoolId = n.longValue();
        } else if (user != null) {
            schoolId = user.getSchoolId();
        }
        // V29 tornou school_id nullable — CEO cross-tenant valido.

        AuditLogEntity log = AuditLogEntity.builder()
                .schoolId(schoolId)
                .userEmail((String) request.get("userEmail"))
                .action((String) request.get("action"))
                .details((String) request.get("details"))
                .ipAddress((String) request.get("ipAddress"))
                .build();
        auditLogRepository.save(log);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
