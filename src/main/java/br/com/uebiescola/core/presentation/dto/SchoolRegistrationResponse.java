package br.com.uebiescola.core.presentation.dto;

import java.util.UUID;

public record SchoolRegistrationResponse(
        Long schoolId,
        UUID schoolExternalId,
        String schoolName,
        String subdomain,
        String adminEmail,
        String message
) {}
