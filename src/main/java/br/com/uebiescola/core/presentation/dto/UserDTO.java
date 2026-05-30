package br.com.uebiescola.core.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.br.CPF;

public record UserDTO(
        String uuid,
        @NotBlank(message = "Nome é obrigatório")
        String name,
        @NotBlank(message = "CPF é obrigatório")
        @CPF(message = "CPF inválido")
        String cpf,
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,
        String password,
        String role,
        Long schoolId,
        Boolean active,
        Long accessLevelId,
        String accessLevelName,
        String photoUrl
) {}
