package br.com.uebiescola.core.application.service;

import br.com.uebiescola.core.infrastructure.config.CacheConfig;
import br.com.uebiescola.core.infrastructure.persistence.entity.GlobalSettingEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaGlobalSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GlobalSettingsService {

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

    private final JpaGlobalSettingRepository settingRepository;

    @Cacheable(value = CacheConfig.CACHE_GLOBAL_SETTINGS_ALL, sync = true)
    public List<GlobalSettingEntity> findAll() {
        return settingRepository.findAll();
    }

    @Cacheable(value = CacheConfig.CACHE_GLOBAL_SETTINGS_BY_CATEGORY, key = "#category", sync = true)
    public List<GlobalSettingEntity> findByCategory(String category) {
        return settingRepository.findByCategory(category);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_GLOBAL_SETTINGS_ALL, allEntries = true),
            @CacheEvict(value = CacheConfig.CACHE_GLOBAL_SETTINGS_BY_CATEGORY, allEntries = true)
    })
    public List<GlobalSettingEntity> upsertAll(Map<String, String> settings) {
        return settings.entrySet().stream()
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
                    return settingRepository.save(entity);
                })
                .toList();
    }
}
