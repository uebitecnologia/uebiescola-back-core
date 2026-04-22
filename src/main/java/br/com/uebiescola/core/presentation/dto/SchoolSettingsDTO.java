package br.com.uebiescola.core.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record SchoolSettingsDTO(
        Boolean twoFactorEnabled,
        Boolean notifyEnrollment,
        Boolean notifyDelinquency,
        Boolean notifyExamReminder,
        String backupSchedule,
        String apiKey,

        // Financeiro
        Integer defaultDueDay,
        String defaultPaymentMethod,
        Boolean dunningEmailEnabled,
        Boolean dunningWhatsappEnabled,
        Boolean dunningPushEnabled,
        Integer dunningDaysFirst,
        Integer dunningDaysSecond,
        Integer dunningDaysThird,

        // Pedagogico
        String gradeScaleType,
        BigDecimal passingGrade,
        Integer minimumAttendancePercent,
        Integer assessmentsPerTerm,

        // Calendario
        LocalDate academicYearStart,
        LocalDate academicYearEnd,
        Integer minimumSchoolDays,

        // Portaria
        Integer qrExpirationMinutes,
        LocalTime gateAllowedStartTime,
        LocalTime gateAllowedEndTime,
        Boolean gateAutoApproval
) {}
