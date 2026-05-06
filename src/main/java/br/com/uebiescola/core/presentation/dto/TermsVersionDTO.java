package br.com.uebiescola.core.presentation.dto;

import br.com.uebiescola.core.domain.model.enums.TermsType;

import java.util.UUID;

public record TermsVersionDTO(
        UUID uuid,
        TermsType type,
        String title,
        String content,
        String version
) {}
