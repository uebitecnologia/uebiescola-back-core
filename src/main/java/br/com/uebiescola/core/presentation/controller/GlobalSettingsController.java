package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.infrastructure.persistence.entity.GlobalSettingEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaGlobalSettingRepository;
import br.com.uebiescola.core.presentation.dto.GlobalSettingDTO;
import jakarta.transaction.Transactional;
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

    private final JpaGlobalSettingRepository settingRepository;

    // Category mapping for known keys
    private static final Map<String, String> KEY_CATEGORIES = Map.ofEntries(
            Map.entry("PAYMENT_PROVIDER", "PAYMENT"),
            Map.entry("PAYMENT_API_KEY", "PAYMENT"),
            Map.entry("PAYMENT_SPLIT_ENABLED", "PAYMENT"),
            Map.entry("WHATSAPP_ENDPOINT", "WHATSAPP"),
            Map.entry("WHATSAPP_TOKEN", "WHATSAPP"),
            Map.entry("SMTP_HOST", "SMTP"),
            Map.entry("SMTP_PORT", "SMTP"),
            Map.entry("SMTP_USER", "SMTP"),
            Map.entry("SMTP_PASSWORD", "SMTP")
    );

    /**
     * Get all global settings, optionally filtered by category.
     */
    @GetMapping
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<List<GlobalSettingDTO>> getAll(
            @RequestParam(required = false) String category) {

        List<GlobalSettingEntity> settings = category != null
                ? settingRepository.findByCategory(category)
                : settingRepository.findAll();

        List<GlobalSettingDTO> result = settings.stream()
                .map(this::toDTO)
                .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * Upsert global settings from a map of key-value pairs.
     */
    @PutMapping
    @PreAuthorize("hasRole('CEO')")
    @Transactional
    public ResponseEntity<List<GlobalSettingDTO>> updateSettings(@RequestBody Map<String, String> settings) {
        List<GlobalSettingDTO> updated = settings.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    String category = KEY_CATEGORIES.getOrDefault(key, "OTHER");

                    GlobalSettingEntity entity = settingRepository.findByKey(key)
                            .orElseGet(() -> GlobalSettingEntity.builder()
                                    .key(key)
                                    .category(category)
                                    .build());

                    entity.setValue(value);
                    entity.setCategory(category);
                    settingRepository.save(entity);

                    return toDTO(entity);
                })
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
