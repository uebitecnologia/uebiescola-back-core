package br.com.uebiescola.core.infrastructure.persistence.repository;

import br.com.uebiescola.core.infrastructure.persistence.entity.ChangelogEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaChangelogEntryRepository extends JpaRepository<ChangelogEntryEntity, Long> {

    Optional<ChangelogEntryEntity> findByUuid(UUID uuid);

    /** Lista para CEO admin (inclui rascunhos). */
    List<ChangelogEntryEntity> findAllByOrderByCreatedAtDesc();

    /** Lista publica: so publicados, ordem cronologica reversa. */
    List<ChangelogEntryEntity> findByPublishedTrueOrderByPublishedAtDesc();
}
