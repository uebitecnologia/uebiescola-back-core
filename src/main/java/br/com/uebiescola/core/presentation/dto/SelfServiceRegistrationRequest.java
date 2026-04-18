package br.com.uebiescola.core.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SelfServiceRegistrationRequest(
        @NotBlank(message = "Nome da escola é obrigatório") String schoolName,
        @NotBlank(message = "Nome do administrador é obrigatório") String adminName,
        @NotBlank(message = "Email é obrigatório") @Email(message = "Email inválido") String adminEmail,
        @NotBlank(message = "Senha é obrigatória") @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres") String adminPassword,
        @NotBlank(message = "CPF é obrigatório") String adminCpf,
        String phone
) {}
