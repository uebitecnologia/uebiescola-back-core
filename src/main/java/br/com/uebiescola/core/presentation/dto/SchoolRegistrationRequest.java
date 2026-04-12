package br.com.uebiescola.core.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SchoolRegistrationRequest(
        @NotBlank String schoolName,
        @NotBlank String cnpj,
        @NotBlank String adminName,
        @NotBlank @Email String adminEmail,
        @NotBlank String adminCpf,
        @NotBlank @Size(min = 6) String adminPassword,
        String phone,
        String subdomain
) {}
