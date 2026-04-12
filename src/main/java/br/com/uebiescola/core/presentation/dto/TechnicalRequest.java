package br.com.uebiescola.core.presentation.dto;

import org.hibernate.validator.constraints.br.CPF;

public record TechnicalRequest(
        String adminName,
        String adminEmail,
        @CPF(message = "CPF inválido")
        String adminCpf,
        String adminPassword,
        String subdomain,
        byte[] logoBytes,
        String logoContentType
) {}