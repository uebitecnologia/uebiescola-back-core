package br.com.uebiescola.core.presentation.dto;

import java.util.UUID;

public record NfacilSignupResponse(
        UUID schoolUuid,
        String schoolSlug,
        String loginUrl,
        String message
) {}
