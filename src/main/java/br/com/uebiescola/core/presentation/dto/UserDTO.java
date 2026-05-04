package br.com.uebiescola.core.presentation.dto;

public record UserDTO(
        Long id,
        String uuid,
        String name,
        String cpf,
        String email,
        String password,
        String role,
        Long schoolId,
        Boolean active,
        Long accessLevelId,
        String accessLevelName
) {}
