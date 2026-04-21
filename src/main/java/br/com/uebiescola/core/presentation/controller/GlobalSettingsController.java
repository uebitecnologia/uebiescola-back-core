package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.service.GlobalSettingsService;
import br.com.uebiescola.core.infrastructure.persistence.entity.GlobalSettingEntity;
import br.com.uebiescola.core.presentation.dto.GlobalSettingDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/global-settings")
@RequiredArgsConstructor
public class GlobalSettingsController {

    private final GlobalSettingsService service;

    @GetMapping
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<List<GlobalSettingDTO>> getAll(
            @RequestParam(required = false) String category) {

        List<GlobalSettingEntity> settings = category != null
                ? service.findByCategory(category)
                : service.findAll();

        List<GlobalSettingDTO> result = settings.stream()
                .map(this::toDTO)
                .toList();

        return ResponseEntity.ok(result);
    }

    @PutMapping
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<List<GlobalSettingDTO>> updateSettings(@RequestBody Map<String, String> settings) {
        List<GlobalSettingDTO> updated = service.upsertAll(settings).stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(updated);
    }

    private GlobalSettingDTO toDTO(GlobalSettingEntity entity) {
        return new GlobalSettingDTO(
                entity.getId(),
                entity.getKey(),
                entity.getValue(),
                entity.getCategory(),
                entity.getUpdatedAt()
        );
    }
}
