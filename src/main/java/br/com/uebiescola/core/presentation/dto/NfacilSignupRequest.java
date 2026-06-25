package br.com.uebiescola.core.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NfacilSignupRequest(
        @NotBlank(message = "CNPJ obrigatorio")
        @Pattern(regexp = "\\d{14}", message = "CNPJ deve ter 14 digitos")
        String cnpj,

        @NotBlank(message = "Razao Social obrigatoria")
        @Size(max = 200)
        String schoolName,

        @Size(max = 200)
        String fantasyName,

        @NotBlank(message = "Nome do administrador obrigatorio")
        @Size(max = 120)
        String adminName,

        @NotBlank(message = "Email obrigatorio")
        @Email(message = "Email invalido")
        String adminEmail,

        @NotBlank(message = "WhatsApp obrigatorio")
        @Pattern(regexp = "\\d{10,11}", message = "Telefone deve ter 10 ou 11 digitos")
        String adminPhone,

        @NotBlank(message = "Cidade obrigatoria")
        String city,

        String cityIbge,

        @NotBlank(message = "Estado obrigatorio")
        @Size(min = 2, max = 2, message = "Estado em sigla (UF) de 2 caracteres")
        String state
) {}
