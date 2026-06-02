package br.com.uebiescola.core.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangelogEntryRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String summary,
        @NotBlank String content,
        @NotBlank @Pattern(regexp = "FEATURE|IMPROVEMENT|FIX|INFRA") String category,
        Boolean published
) {}
