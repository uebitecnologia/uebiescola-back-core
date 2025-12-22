package br.com.uebiescola.core.presentation.dto;

import br.com.uebiescola.core.domain.model.UserRole;
import java.util.UUID;

public record LoginResponse(
        String token,
        String name,
        UserRole role,
        UUID userExternalId
) {}