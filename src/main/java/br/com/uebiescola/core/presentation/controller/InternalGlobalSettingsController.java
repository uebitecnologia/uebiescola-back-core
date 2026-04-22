package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.service.GlobalSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Endpoint INTERNO (service-to-service) para acesso as global settings.
 * Nao expoe via nginx (/v1/internal/* nao tem location mapeada), entao
 * so acessivel dentro da rede Docker.
 *
 * Usado pelo notification-service para ler VAPID/SMTP/WhatsApp que o CEO
 * configura no admin.
 */
@RestController
@RequestMapping("/api/v1/internal/global-settings")
@RequiredArgsConstructor
public class InternalGlobalSettingsController {

    private final GlobalSettingsService service;

    @GetMapping("/values")
    public ResponseEntity<Map<String, String>> getAllValues() {
        Map<String, String> values = service.findAll().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue(),
                        (a, b) -> a
                ));
        return ResponseEntity.ok(values);
    }
}
