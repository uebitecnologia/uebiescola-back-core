package br.com.uebiescola.core.presentation.dto;

public record AccessLevelDTO(
        Long id,
        Long schoolId,
        String name,
        String description,
        String permissions,
        Boolean active,
        Boolean systemDefault
) {}
