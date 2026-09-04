package br.com.uebiescola.core.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
        // A-7 AUDITORIAADMINPLATAFORMA 03/09/2026: WRITE_ONLY — aceita no POST/PUT
        // (fluxo de criacao/atualizacao de usuario), mas nunca sai serializado
        // em GET. Chrome flagou que o campo estava no contrato do DTO, e mesmo
        // vindo vazio hoje qualquer caminho que popule a entidade passaria a
        // vazar o hash.
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String password,
        String role,
        Long schoolId,
        Boolean active,
        Long accessLevelId,
        String accessLevelName,
        String photoUrl
) {}
