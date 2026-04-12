package br.com.uebiescola.core.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

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

    // --- Segurança ---
    @Builder.Default
    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled = false;

    // --- Notificações ---
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
}
