package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.service.PublicStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/**
 * Endpoint publico (sem auth) consumido pela status page estatica em
 * https://status.uebiescola.com.br
 *
 * Retorna snapshot consolidado: status atual de cada componente, historico
 * 90 dias, incidentes recentes e proxima manutencao programada.
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicStatusController {

    private final PublicStatusService service;

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic())
                .body(service.getSnapshot());
    }
}
