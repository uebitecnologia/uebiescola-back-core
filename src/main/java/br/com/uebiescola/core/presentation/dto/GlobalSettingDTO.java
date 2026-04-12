package br.com.uebiescola.core.presentation.dto;

import java.time.LocalDateTime;

public record GlobalSettingDTO(
        Long id,
        String key,
        String value,
        String category,
        LocalDateTime updatedAt
) {}
