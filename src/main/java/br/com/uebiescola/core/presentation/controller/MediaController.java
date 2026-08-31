package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.service.MediaUploadService;
import br.com.uebiescola.core.infrastructure.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Endpoint genérico de upload de imagens (avatares).
 * Retorna apenas a URL pública resultante; persistir o link na entidade
 * é responsabilidade do caller (via PATCH no controller específico).
 */
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MediaController {

    private final MediaUploadService mediaUploadService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("kind") String kind,
            @RequestParam(value = "entityUuid", required = false) String entityUuid,
            @AuthenticationPrincipal AuthenticatedUser user) {

        Long schoolId = user != null && user.getSchoolId() != null
                ? user.getSchoolId() : 0L;

        String url = mediaUploadService.upload(
                file,
                schoolId,
                MediaUploadService.Kind.fromString(kind),
                entityUuid);

        return ResponseEntity.ok(Map.of("url", url));
    }
}
