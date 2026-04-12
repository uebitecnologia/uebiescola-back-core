package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.infrastructure.persistence.entity.AuditLogEntity;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolSettingsEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaAuditLogRepository;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolRepository;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolSettingsRepository;
import br.com.uebiescola.core.infrastructure.security.AuthenticatedUser;
import br.com.uebiescola.core.presentation.dto.AuditLogDTO;
import br.com.uebiescola.core.presentation.dto.SchoolSettingsDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schools/{schoolId}/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final JpaSchoolSettingsRepository settingsRepository;
    private final JpaAuditLogRepository auditLogRepository;
    private final JpaSchoolRepository schoolRepository;

    // ===================== SETTINGS =====================

    @GetMapping
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    public ResponseEntity<SchoolSettingsDTO> getSettings(
            @PathVariable Long schoolId,
            @AuthenticationPrincipal AuthenticatedUser user) {

        if (!hasAccess(user, schoolId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        SchoolSettingsEntity settings = settingsRepository.findById(schoolId)
                .orElseGet(() -> createDefaultSettings(schoolId));

        return ResponseEntity.ok(toDTO(settings));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    @Transactional
    public ResponseEntity<SchoolSettingsDTO> updateSettings(
            @PathVariable Long schoolId,
            @RequestBody SchoolSettingsDTO dto,
            @AuthenticationPrincipal AuthenticatedUser user) {

        if (!hasAccess(user, schoolId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        SchoolSettingsEntity settings = settingsRepository.findById(schoolId)
                .orElseGet(() -> createDefaultSettings(schoolId));

        if (dto.twoFactorEnabled() != null) settings.setTwoFactorEnabled(dto.twoFactorEnabled());
        if (dto.notifyEnrollment() != null) settings.setNotifyEnrollment(dto.notifyEnrollment());
        if (dto.notifyDelinquency() != null) settings.setNotifyDelinquency(dto.notifyDelinquency());
        if (dto.notifyExamReminder() != null) settings.setNotifyExamReminder(dto.notifyExamReminder());
        if (dto.backupSchedule() != null) settings.setBackupSchedule(dto.backupSchedule());

        settingsRepository.save(settings);

        // Registra no log de auditoria
        logAction(schoolId, user.getEmail(), "Alterou Configurações", "Configurações do sistema atualizadas");

        return ResponseEntity.ok(toDTO(settings));
    }

    @PostMapping("/generate-api-key")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    @Transactional
    public ResponseEntity<SchoolSettingsDTO> generateApiKey(
            @PathVariable Long schoolId,
            @AuthenticationPrincipal AuthenticatedUser user) {

        if (!hasAccess(user, schoolId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        SchoolSettingsEntity settings = settingsRepository.findById(schoolId)
                .orElseGet(() -> createDefaultSettings(schoolId));

        String newKey = "uebi_live_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        settings.setApiKey(newKey);
        settingsRepository.save(settings);

        logAction(schoolId, user.getEmail(), "Gerou Nova API Key", "Chave de API regenerada");

        return ResponseEntity.ok(toDTO(settings));
    }

    // ===================== AUDIT LOG =====================

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    public ResponseEntity<List<AuditLogDTO>> getAuditLogs(
            @PathVariable Long schoolId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal AuthenticatedUser user) {

        if (!hasAccess(user, schoolId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        var logs = auditLogRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId, PageRequest.of(page, size));

        List<AuditLogDTO> result = logs.getContent().stream()
                .map(log -> new AuditLogDTO(log.getId(), log.getUserEmail(), log.getAction(), log.getDetails(), log.getCreatedAt()))
                .toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping("/audit-logs")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    public ResponseEntity<Void> createAuditLog(
            @PathVariable Long schoolId,
            @RequestBody AuditLogDTO dto,
            @AuthenticationPrincipal AuthenticatedUser user) {

        if (!hasAccess(user, schoolId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        logAction(schoolId, user.getEmail(), dto.action(), dto.details());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ===================== HELPERS =====================

    private boolean hasAccess(AuthenticatedUser user, Long schoolId) {
        return user.getRole().contains("CEO") || schoolId.equals(user.getSchoolId());
    }

    private SchoolSettingsEntity createDefaultSettings(Long schoolId) {
        var school = schoolRepository.findById(schoolId).orElse(null);
        if (school == null) return SchoolSettingsEntity.builder().schoolId(schoolId).build();

        SchoolSettingsEntity settings = SchoolSettingsEntity.builder()
                .school(school)
                .build();
        return settingsRepository.save(settings);
    }

    private void logAction(Long schoolId, String email, String action, String details) {
        AuditLogEntity log = AuditLogEntity.builder()
                .schoolId(schoolId)
                .userEmail(email)
                .action(action)
                .details(details)
                .build();
        auditLogRepository.save(log);
    }

    private SchoolSettingsDTO toDTO(SchoolSettingsEntity entity) {
        return new SchoolSettingsDTO(
                entity.getTwoFactorEnabled(),
                entity.getNotifyEnrollment(),
                entity.getNotifyDelinquency(),
                entity.getNotifyExamReminder(),
                entity.getBackupSchedule(),
                entity.getApiKey()
        );
    }
}
