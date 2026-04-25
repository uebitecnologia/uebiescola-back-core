package br.com.uebiescola.core.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

public record SchoolDeletedEvent(
        Long schoolId,
        UUID externalId,
        String schoolName,
        String subdomain,
        Instant deletedAt
) {}
