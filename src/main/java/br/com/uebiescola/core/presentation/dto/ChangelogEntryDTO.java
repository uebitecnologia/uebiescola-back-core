package br.com.uebiescola.core.presentation.dto;

import br.com.uebiescola.core.infrastructure.persistence.entity.ChangelogEntryEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChangelogEntryDTO(
        UUID uuid,
        String title,
        String summary,
        String content,
        String category,
        Boolean published,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ChangelogEntryDTO from(ChangelogEntryEntity e) {
        return new ChangelogEntryDTO(
                e.getUuid(), e.getTitle(), e.getSummary(), e.getContent(),
                e.getCategory(), e.getPublished(), e.getPublishedAt(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    /** Variante reduzida para o endpoint publico — sem timestamps internos. */
    public static ChangelogEntryDTO publicView(ChangelogEntryEntity e) {
        return new ChangelogEntryDTO(
                e.getUuid(), e.getTitle(), e.getSummary(), e.getContent(),
                e.getCategory(), true, e.getPublishedAt(), null, null
        );
    }
}
