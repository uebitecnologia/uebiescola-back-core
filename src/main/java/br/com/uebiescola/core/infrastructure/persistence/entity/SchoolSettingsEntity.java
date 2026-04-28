package br.com.uebiescola.core.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "school_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolSettingsEntity {

    @Id
    @Column(name = "school_id")
    private Long schoolId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "school_id")
    private SchoolEntity school;

    // --- Seguranca ---
    @Builder.Default
    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled = false;

    // --- Notificacoes ---
    @Builder.Default
    @Column(name = "notify_enrollment")
    private Boolean notifyEnrollment = true;

    @Builder.Default
    @Column(name = "notify_delinquency")
    private Boolean notifyDelinquency = false;

    @Builder.Default
    @Column(name = "notify_exam_reminder")
    private Boolean notifyExamReminder = true;

    // --- Infra & Backup ---
    @Builder.Default
    @Column(name = "backup_schedule")
    private String backupSchedule = "DAILY_04";

    // --- API ---
    @Column(name = "api_key")
    private String apiKey;

    // ==================== FINANCEIRO ====================
    @Builder.Default
    @Column(name = "default_due_day")
    private Integer defaultDueDay = 10;

    @Builder.Default
    @Column(name = "default_payment_method")
    private String defaultPaymentMethod = "PIX";

    @Builder.Default
    @Column(name = "dunning_email_enabled")
    private Boolean dunningEmailEnabled = true;

    @Builder.Default
    @Column(name = "dunning_whatsapp_enabled")
    private Boolean dunningWhatsappEnabled = true;

    @Builder.Default
    @Column(name = "dunning_push_enabled")
    private Boolean dunningPushEnabled = true;

    @Builder.Default
    @Column(name = "dunning_days_first")
    private Integer dunningDaysFirst = 3;

    @Builder.Default
    @Column(name = "dunning_days_second")
    private Integer dunningDaysSecond = 7;

    @Builder.Default
    @Column(name = "dunning_days_third")
    private Integer dunningDaysThird = 15;

    @Column(name = "discount_percent")
    private BigDecimal discountPercent;

    @Column(name = "discount_limit_days")
    private Integer discountLimitDays;

    @Builder.Default
    @Column(name = "accept_pix")
    private Boolean acceptPix = true;

    @Builder.Default
    @Column(name = "accept_boleto")
    private Boolean acceptBoleto = true;

    @Builder.Default
    @Column(name = "accept_card")
    private Boolean acceptCard = true;

    @Builder.Default
    @Column(name = "max_installments")
    private Integer maxInstallments = 12;

    @Builder.Default
    @Column(name = "nfse_enabled")
    private Boolean nfseEnabled = false;

    @Builder.Default
    @Column(name = "nfse_auto_emit")
    private Boolean nfseAutoEmit = false;

    @Column(name = "invoice_description")
    private String invoiceDescription;

    // ==================== PEDAGOGICO ====================
    @Builder.Default
    @Column(name = "grade_scale_type")
    private String gradeScaleType = "NUMERIC_0_10";

    @Builder.Default
    @Column(name = "passing_grade")
    private BigDecimal passingGrade = new BigDecimal("6.00");

    @Builder.Default
    @Column(name = "minimum_attendance_percent")
    private Integer minimumAttendancePercent = 75;

    @Builder.Default
    @Column(name = "assessments_per_term")
    private Integer assessmentsPerTerm = 2;

    // ==================== CALENDARIO ====================
    @Column(name = "academic_year_start")
    private LocalDate academicYearStart;

    @Column(name = "academic_year_end")
    private LocalDate academicYearEnd;

    @Builder.Default
    @Column(name = "minimum_school_days")
    private Integer minimumSchoolDays = 200;

    // ==================== PORTARIA ====================
    @Builder.Default
    @Column(name = "qr_expiration_minutes")
    private Integer qrExpirationMinutes = 30;

    @Builder.Default
    @Column(name = "gate_allowed_start_time")
    private LocalTime gateAllowedStartTime = LocalTime.of(6, 0);

    @Builder.Default
    @Column(name = "gate_allowed_end_time")
    private LocalTime gateAllowedEndTime = LocalTime.of(19, 0);

    @Builder.Default
    @Column(name = "gate_auto_approval")
    private Boolean gateAutoApproval = true;
}
