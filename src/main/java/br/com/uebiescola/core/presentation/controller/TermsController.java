package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.usecase.AcceptTermsUseCase;
import br.com.uebiescola.core.application.usecase.CheckTermsStatusUseCase;
import br.com.uebiescola.core.application.usecase.ManageTermsVersionUseCase;
import br.com.uebiescola.core.domain.exception.ResourceNotFoundException;
import br.com.uebiescola.core.domain.model.TermsAcceptance;
import br.com.uebiescola.core.domain.model.TermsVersion;
import br.com.uebiescola.core.domain.model.enums.TermsType;
import br.com.uebiescola.core.domain.repository.TermsVersionRepository;
import br.com.uebiescola.core.infrastructure.persistence.entity.UserEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaUserRepository;
import br.com.uebiescola.core.infrastructure.security.AuthenticatedUser;
import br.com.uebiescola.core.presentation.dto.AcceptTermsRequest;
import br.com.uebiescola.core.presentation.dto.TermsVersionDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TermsController {

    private final ManageTermsVersionUseCase manageTermsVersionUseCase;
    private final AcceptTermsUseCase acceptTermsUseCase;
    private final CheckTermsStatusUseCase checkTermsStatusUseCase;
    private final JpaUserRepository userRepository;
    private final TermsVersionRepository termsVersionRepository;

    private Long resolveTermsVersionId(String idOrUuid) {
        if (idOrUuid == null || idOrUuid.isBlank()) {
            throw new ResourceNotFoundException("Identificador ausente");
        }
        // Try UUID first
        try {
            UUID uuid = UUID.fromString(idOrUuid);
            return termsVersionRepository.findByUuid(uuid)
                    .map(TermsVersion::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("Versão de termos não encontrada: " + uuid));
        } catch (IllegalArgumentException ignored) {
            // fallback: numeric Long
        }
        try {
            return Long.parseLong(idOrUuid);
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException("Identificador inválido: " + idOrUuid);
        }
    }

    // ===================== PUBLIC ENDPOINTS =====================

    @GetMapping("/public/terms/latest")
    public ResponseEntity<TermsVersion> getLatestActiveByType(@RequestParam TermsType type) {
        return manageTermsVersionUseCase.getLatestActive(type)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/public/terms/latest/all")
    public ResponseEntity<List<TermsVersion>> getAllLatestActive() {
        List<TermsVersion> result = new ArrayList<>();
        for (TermsType type : TermsType.values()) {
            manageTermsVersionUseCase.getLatestActive(type).ifPresent(result::add);
        }
        return ResponseEntity.ok(result);
    }

    // ===================== AUTHENTICATED ENDPOINTS =====================

    @PostMapping("/terms/accept")
    public ResponseEntity<?> acceptTerms(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestBody AcceptTermsRequest request,
            HttpServletRequest httpRequest) {

        UserEntity user = userRepository.findByEmail(auth.getEmail()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuário não encontrado"));
        }

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        TermsAcceptance acceptance = acceptTermsUseCase.execute(
                user.getId(),
                user.getSchoolId(),
                request.termsVersionId(),
                ipAddress,
                userAgent
        );

        return ResponseEntity.ok(Map.of(
                "message", "Termos aceitos com sucesso",
                "acceptedAt", acceptance.getAcceptedAt().toString()
        ));
    }

    @GetMapping("/terms/status")
    public ResponseEntity<List<TermsVersion>> getTermsStatus(
            @AuthenticationPrincipal AuthenticatedUser auth) {

        UserEntity user = userRepository.findByEmail(auth.getEmail()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<TermsVersion> pendingTerms = checkTermsStatusUseCase.getPendingTerms(user.getId());
        return ResponseEntity.ok(pendingTerms);
    }

    // ===================== CEO-ONLY ENDPOINTS =====================

    @GetMapping("/terms/versions")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<List<TermsVersion>> listAllVersions() {
        return ResponseEntity.ok(manageTermsVersionUseCase.listAll());
    }

    @PostMapping("/terms/versions")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<TermsVersion> createVersion(
            @RequestBody TermsVersionDTO dto,
            @AuthenticationPrincipal AuthenticatedUser auth) {

        TermsVersion termsVersion = TermsVersion.builder()
                .type(dto.type())
                .title(dto.title())
                .content(dto.content())
                .version(dto.version())
                .createdBy(auth.getEmail())
                .build();

        TermsVersion created = manageTermsVersionUseCase.create(termsVersion);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/terms/versions/{uuid}")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<TermsVersion> updateVersion(
            @PathVariable("uuid") String idOrUuid,
            @RequestBody TermsVersionDTO dto) {

        TermsVersion updated = TermsVersion.builder()
                .type(dto.type())
                .title(dto.title())
                .content(dto.content())
                .version(dto.version())
                .build();

        Long id = resolveTermsVersionId(idOrUuid);
        TermsVersion result = manageTermsVersionUseCase.update(id, updated);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/terms/versions/{uuid}/activate")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<TermsVersion> activateVersion(@PathVariable("uuid") String idOrUuid) {
        Long id = resolveTermsVersionId(idOrUuid);
        TermsVersion activated = manageTermsVersionUseCase.activate(id);
        return ResponseEntity.ok(activated);
    }

    // ===================== HELPERS =====================

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
