package br.com.uebiescola.core.presentation.dto;

import java.time.LocalDateTime;

public record AuditLogDTO(
        String userEmail,
        String action,
        String details,
        LocalDateTime createdAt
) {}
