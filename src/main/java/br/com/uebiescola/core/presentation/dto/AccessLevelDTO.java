package br.com.uebiescola.core.presentation.dto;

import java.util.UUID;

public record AccessLevelDTO(
        Long id,
        UUID uuid,
        Long schoolId,
        String name,
        String description,
        String permissions,
        Boolean active,
        Boolean systemDefault
) {}
