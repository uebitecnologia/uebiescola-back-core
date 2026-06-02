package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.infrastructure.persistence.entity.ChangelogEntryEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaChangelogEntryRepository;
import br.com.uebiescola.core.presentation.dto.ChangelogEntryDTO;
import br.com.uebiescola.core.presentation.dto.ChangelogEntryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Changelog publico — CEO publica novidades; site marketing exibe em
 * /novidades. Admin escola pode consumir o mesmo endpoint publico no futuro.
 */
@RestController
@RequiredArgsConstructor
public class ChangelogController {

    private final JpaChangelogEntryRepository repo;

    // ---------- Publico (sem auth) ----------

    @GetMapping("/api/v1/public/changelog")
    public ResponseEntity<List<ChangelogEntryDTO>> listPublic() {
        var entries = repo.findByPublishedTrueOrderByPublishedAtDesc()
                .stream().map(ChangelogEntryDTO::publicView).toList();
        return ResponseEntity.ok(entries);
    }

    // ---------- CEO admin (autenticado + role) ----------

    @GetMapping("/api/v1/changelog")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<List<ChangelogEntryDTO>> listAll() {
        var entries = repo.findAllByOrderByCreatedAtDesc()
                .stream().map(ChangelogEntryDTO::from).toList();
        return ResponseEntity.ok(entries);
    }

    @PostMapping("/api/v1/changelog")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<ChangelogEntryDTO> create(@RequestBody @Valid ChangelogEntryRequest req) {
        boolean publish = Boolean.TRUE.equals(req.published());
        var e = ChangelogEntryEntity.builder()
                .title(req.title())
                .summary(req.summary())
                .content(req.content())
                .category(req.category())
                .published(publish)
                .publishedAt(publish ? LocalDateTime.now() : null)
                .build();
        return ResponseEntity.ok(ChangelogEntryDTO.from(repo.save(e)));
    }

    @PutMapping("/api/v1/changelog/{uuid}")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<?> update(@PathVariable UUID uuid, @RequestBody @Valid ChangelogEntryRequest req) {
        var opt = repo.findByUuid(uuid);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        var e = opt.get();
        e.setTitle(req.title());
        e.setSummary(req.summary());
        e.setContent(req.content());
        e.setCategory(req.category());
        // Toggle de publicacao seta publishedAt na primeira vez que vira true,
        // mas mantem o original em re-edicoes pra preservar a data oficial.
        boolean wasPublished = Boolean.TRUE.equals(e.getPublished());
        boolean newPublished = Boolean.TRUE.equals(req.published());
        e.setPublished(newPublished);
        if (newPublished && !wasPublished) {
            e.setPublishedAt(LocalDateTime.now());
        } else if (!newPublished) {
            e.setPublishedAt(null);
        }
        return ResponseEntity.ok(ChangelogEntryDTO.from(repo.save(e)));
    }

    @DeleteMapping("/api/v1/changelog/{uuid}")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<?> delete(@PathVariable UUID uuid) {
        var opt = repo.findByUuid(uuid);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        repo.delete(opt.get());
        return ResponseEntity.ok(Map.of("deleted", true));
    }
}
