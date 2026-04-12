package br.com.uebiescola.core.presentation.dto;

public record SchoolSettingsDTO(
        Boolean twoFactorEnabled,
        Boolean notifyEnrollment,
        Boolean notifyDelinquency,
        Boolean notifyExamReminder,
        String backupSchedule,
        String apiKey
) {}
