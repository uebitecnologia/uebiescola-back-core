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
@RequestMapping("/api/v1/schools/{schoolUuid}/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final JpaSchoolSettingsRepository settingsRepository;
    private final JpaAuditLogRepository auditLogRepository;
    private final JpaSchoolRepository schoolRepository;

    private Long resolveSchoolId(String idOrUuid) {
        if (idOrUuid == null || idOrUuid.isBlank()) return null;
        // Try UUID first
        try {
            UUID uuid = UUID.fromString(idOrUuid);
            return schoolRepository.findByUuid(uuid)
                    .map(s -> s.getId())
                    .orElse(null);
        } catch (IllegalArgumentException ignored) {
            // fallback: numeric Long
        }
        try {
            Long id = Long.parseLong(idOrUuid);
            return schoolRepository.findById(id).map(s -> s.getId()).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ===================== SETTINGS =====================

    @GetMapping
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    public ResponseEntity<SchoolSettingsDTO> getSettings(
            @PathVariable("schoolUuid") String schoolIdOrUuid,
            @AuthenticationPrincipal AuthenticatedUser user) {

        Long schoolId = resolveSchoolId(schoolIdOrUuid);
        if (schoolId == null) return ResponseEntity.notFound().build();
        if (!hasAccess(user, schoolId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        SchoolSettingsEntity settings = settingsRepository.findById(schoolId)
                .orElseGet(() -> createDefaultSettings(schoolId));

        return ResponseEntity.ok(toDTO(settings));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    @Transactional
    public ResponseEntity<SchoolSettingsDTO> updateSettings(
            @PathVariable("schoolUuid") String schoolIdOrUuid,
            @RequestBody SchoolSettingsDTO dto,
            @AuthenticationPrincipal AuthenticatedUser user) {

        Long schoolId = resolveSchoolId(schoolIdOrUuid);
        if (schoolId == null) return ResponseEntity.notFound().build();
        if (!hasAccess(user, schoolId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        SchoolSettingsEntity settings = settingsRepository.findById(schoolId)
                .orElseGet(() -> createDefaultSettings(schoolId));

        if (dto.twoFactorEnabled() != null) settings.setTwoFactorEnabled(dto.twoFactorEnabled());
        if (dto.notifyEnrollment() != null) settings.setNotifyEnrollment(dto.notifyEnrollment());
        if (dto.notifyDelinquency() != null) settings.setNotifyDelinquency(dto.notifyDelinquency());
        if (dto.notifyExamReminder() != null) settings.setNotifyExamReminder(dto.notifyExamReminder());
        if (dto.backupSchedule() != null) settings.setBackupSchedule(dto.backupSchedule());

        // Financeiro
        if (dto.defaultDueDay() != null) settings.setDefaultDueDay(dto.defaultDueDay());
        if (dto.defaultPaymentMethod() != null) settings.setDefaultPaymentMethod(dto.defaultPaymentMethod());
        if (dto.dunningEmailEnabled() != null) settings.setDunningEmailEnabled(dto.dunningEmailEnabled());
        if (dto.dunningWhatsappEnabled() != null) settings.setDunningWhatsappEnabled(dto.dunningWhatsappEnabled());
        if (dto.dunningPushEnabled() != null) settings.setDunningPushEnabled(dto.dunningPushEnabled());
        if (dto.dunningDaysFirst() != null) settings.setDunningDaysFirst(dto.dunningDaysFirst());
        if (dto.dunningDaysSecond() != null) settings.setDunningDaysSecond(dto.dunningDaysSecond());
        if (dto.dunningDaysThird() != null) settings.setDunningDaysThird(dto.dunningDaysThird());
        if (dto.discountPercent() != null) settings.setDiscountPercent(dto.discountPercent());
        if (dto.discountLimitDays() != null) settings.setDiscountLimitDays(dto.discountLimitDays());
        if (dto.acceptPix() != null) settings.setAcceptPix(dto.acceptPix());
        if (dto.acceptBoleto() != null) settings.setAcceptBoleto(dto.acceptBoleto());
        if (dto.acceptCard() != null) settings.setAcceptCard(dto.acceptCard());
        if (dto.maxInstallments() != null) settings.setMaxInstallments(dto.maxInstallments());
        if (dto.nfseEnabled() != null) settings.setNfseEnabled(dto.nfseEnabled());
        if (dto.nfseAutoEmit() != null) settings.setNfseAutoEmit(dto.nfseAutoEmit());
        if (dto.invoiceDescription() != null) settings.setInvoiceDescription(dto.invoiceDescription());

        // Pedagogico
        if (dto.gradeScaleType() != null) settings.setGradeScaleType(dto.gradeScaleType());
        if (dto.passingGrade() != null) settings.setPassingGrade(dto.passingGrade());
        if (dto.minimumAttendancePercent() != null) settings.setMinimumAttendancePercent(dto.minimumAttendancePercent());
        if (dto.assessmentsPerTerm() != null) settings.setAssessmentsPerTerm(dto.assessmentsPerTerm());

        // Calendario
        if (dto.academicYearStart() != null) settings.setAcademicYearStart(dto.academicYearStart());
        if (dto.academicYearEnd() != null) settings.setAcademicYearEnd(dto.academicYearEnd());
        if (dto.minimumSchoolDays() != null) settings.setMinimumSchoolDays(dto.minimumSchoolDays());

        // Portaria
        if (dto.qrExpirationMinutes() != null) settings.setQrExpirationMinutes(dto.qrExpirationMinutes());
        if (dto.gateAllowedStartTime() != null) settings.setGateAllowedStartTime(dto.gateAllowedStartTime());
        if (dto.gateAllowedEndTime() != null) settings.setGateAllowedEndTime(dto.gateAllowedEndTime());
        if (dto.gateAutoApproval() != null) settings.setGateAutoApproval(dto.gateAutoApproval());

        settingsRepository.save(settings);

        // Registra no log de auditoria
        logAction(schoolId, user.getEmail(), "Alterou Configurações", "Configurações do sistema atualizadas");

        return ResponseEntity.ok(toDTO(settings));
    }

    @PostMapping("/generate-api-key")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    @Transactional
    public ResponseEntity<SchoolSettingsDTO> generateApiKey(
            @PathVariable("schoolUuid") String schoolIdOrUuid,
            @AuthenticationPrincipal AuthenticatedUser user) {

        Long schoolId = resolveSchoolId(schoolIdOrUuid);
        if (schoolId == null) return ResponseEntity.notFound().build();
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
            @PathVariable("schoolUuid") String schoolIdOrUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal AuthenticatedUser user) {

        Long schoolId = resolveSchoolId(schoolIdOrUuid);
        if (schoolId == null) return ResponseEntity.notFound().build();
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
            @PathVariable("schoolUuid") String schoolIdOrUuid,
            @RequestBody AuditLogDTO dto,
            @AuthenticationPrincipal AuthenticatedUser user) {

        Long schoolId = resolveSchoolId(schoolIdOrUuid);
        if (schoolId == null) return ResponseEntity.notFound().build();
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

    private SchoolSettingsDTO toDTO(SchoolSettingsEntity e) {
        return new SchoolSettingsDTO(
                e.getTwoFactorEnabled(),
                e.getNotifyEnrollment(),
                e.getNotifyDelinquency(),
                e.getNotifyExamReminder(),
                e.getBackupSchedule(),
                e.getApiKey(),
                e.getDefaultDueDay(),
                e.getDefaultPaymentMethod(),
                e.getDunningEmailEnabled(),
                e.getDunningWhatsappEnabled(),
                e.getDunningPushEnabled(),
                e.getDunningDaysFirst(),
                e.getDunningDaysSecond(),
                e.getDunningDaysThird(),
                e.getDiscountPercent(),
                e.getDiscountLimitDays(),
                e.getAcceptPix(),
                e.getAcceptBoleto(),
                e.getAcceptCard(),
                e.getMaxInstallments(),
                e.getNfseEnabled(),
                e.getNfseAutoEmit(),
                e.getInvoiceDescription(),
                e.getGradeScaleType(),
                e.getPassingGrade(),
                e.getMinimumAttendancePercent(),
                e.getAssessmentsPerTerm(),
                e.getAcademicYearStart(),
                e.getAcademicYearEnd(),
                e.getMinimumSchoolDays(),
                e.getQrExpirationMinutes(),
                e.getGateAllowedStartTime(),
                e.getGateAllowedEndTime(),
                e.getGateAutoApproval()
        );
    }
}
