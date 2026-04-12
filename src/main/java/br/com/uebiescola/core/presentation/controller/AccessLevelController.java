package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.infrastructure.persistence.entity.AccessLevelEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaAccessLevelRepository;
import br.com.uebiescola.core.infrastructure.security.AuthenticatedUser;
import br.com.uebiescola.core.presentation.dto.AccessLevelDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/access-levels")
@RequiredArgsConstructor
public class AccessLevelController {

    private final JpaAccessLevelRepository repository;

    @GetMapping
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    public ResponseEntity<List<AccessLevelDTO>> list(
            @RequestParam(value = "schoolId", required = false) Long requestSchoolId,
            @AuthenticationPrincipal AuthenticatedUser user) {

        Long schoolId = resolveSchoolId(user, requestSchoolId);
        if (schoolId == null) return ResponseEntity.badRequest().build();

        List<AccessLevelDTO> levels = repository.findAllBySchoolIdOrderByNameAsc(schoolId).stream()
                .map(this::toDTO)
                .toList();

        return ResponseEntity.ok(levels);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    public ResponseEntity<List<AccessLevelDTO>> listActive(
            @RequestParam(value = "schoolId", required = false) Long requestSchoolId,
            @AuthenticationPrincipal AuthenticatedUser user) {

        Long schoolId = resolveSchoolId(user, requestSchoolId);
        if (schoolId == null) return ResponseEntity.badRequest().build();

        List<AccessLevelDTO> levels = repository.findAllBySchoolIdAndActiveTrue(schoolId).stream()
                .map(this::toDTO)
                .toList();

        return ResponseEntity.ok(levels);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    public ResponseEntity<AccessLevelDTO> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return repository.findById(id)
                .filter(e -> hasAccess(user, e.getSchoolId()))
                .map(e -> ResponseEntity.ok(toDTO(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    @Transactional
    public ResponseEntity<AccessLevelDTO> create(
            @RequestBody AccessLevelDTO dto,
            @AuthenticationPrincipal AuthenticatedUser user) {

        Long schoolId = resolveSchoolId(user, dto.schoolId());
        if (schoolId == null) return ResponseEntity.badRequest().build();

        AccessLevelEntity entity = AccessLevelEntity.builder()
                .schoolId(schoolId)
                .name(dto.name())
                .description(dto.description())
                .permissions(dto.permissions())
                .active(true)
                .systemDefault(false)
                .build();

        AccessLevelEntity saved = repository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    @Transactional
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody AccessLevelDTO dto,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return repository.findById(id)
                .filter(e -> hasAccess(user, e.getSchoolId()))
                .map(entity -> {
                    if (entity.getSystemDefault()) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body((Object) Map.of("error", "Não é possível editar nível padrão do sistema"));
                    }
                    if (dto.name() != null) entity.setName(dto.name());
                    if (dto.description() != null) entity.setDescription(dto.description());
                    if (dto.permissions() != null) entity.setPermissions(dto.permissions());

                    AccessLevelEntity saved = repository.save(entity);
                    return ResponseEntity.ok((Object) toDTO(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    @Transactional
    public ResponseEntity<AccessLevelDTO> toggleStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return repository.findById(id)
                .filter(e -> hasAccess(user, e.getSchoolId()))
                .map(entity -> {
                    entity.setActive(body.getOrDefault("active", !entity.getActive()));
                    AccessLevelEntity saved = repository.save(entity);
                    return ResponseEntity.ok(toDTO(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    @Transactional
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return repository.findById(id)
                .filter(e -> hasAccess(user, e.getSchoolId()))
                .map(entity -> {
                    if (entity.getSystemDefault()) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build();
                    }
                    repository.delete(entity);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ===================== HELPERS =====================

    private Long resolveSchoolId(AuthenticatedUser user, Long requestSchoolId) {
        if (user.getRole().contains("CEO")) {
            // CEO can pass schoolId as param, or it comes from X-School-Id header
            return requestSchoolId != null ? requestSchoolId : user.getSchoolId();
        }
        return user.getSchoolId();
    }

    private boolean hasAccess(AuthenticatedUser user, Long schoolId) {
        return user.getRole().contains("CEO") || schoolId.equals(user.getSchoolId());
    }

    private AccessLevelDTO toDTO(AccessLevelEntity entity) {
        return new AccessLevelDTO(
                entity.getId(),
                entity.getSchoolId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPermissions(),
                entity.getActive(),
                entity.getSystemDefault()
        );
    }
}
