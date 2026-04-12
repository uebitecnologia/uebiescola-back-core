package br.com.uebiescola.core.presentation.dto;

import java.time.LocalDateTime;

public record AuditLogResponseDTO(
        Long id,
        Long schoolId,
        String schoolName,
        String userEmail,
        String action,
        String details,
        LocalDateTime timestamp
) {}
