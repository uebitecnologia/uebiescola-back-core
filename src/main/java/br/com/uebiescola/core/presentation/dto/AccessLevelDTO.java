package br.com.uebiescola.core.presentation.dto;

import java.util.UUID;

public record AccessLevelDTO(
        // Long id exposto pra frontend usar em selects/payloads legacy.
        // UUID continua sendo a chave pública.
        Long id,
        UUID uuid,
        Long schoolId,
        String name,
        String description,
        String permissions,
        Boolean active,
        Boolean systemDefault
) {}
