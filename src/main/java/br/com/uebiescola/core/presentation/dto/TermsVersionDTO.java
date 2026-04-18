package br.com.uebiescola.core.presentation.dto;

import br.com.uebiescola.core.domain.model.enums.TermsType;

public record TermsVersionDTO(
        TermsType type,
        String title,
        String content,
        String version
) {}
