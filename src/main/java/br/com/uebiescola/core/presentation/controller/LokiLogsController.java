package br.com.uebiescola.core.presentation.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Proxy CEO-only para consultas ao Loki.
 *
 * O Loki nao e exposto publicamente (so na rede interna do Docker), entao
 * este endpoint serve como gateway autenticado com JWT + role CEO.
 */
@RestController
@RequestMapping("/api/v1/admin/logs")
@Slf4j
public class LokiLogsController {

    private final RestTemplate restTemplate;
    private final String lokiUrl;

    public LokiLogsController(
            RestTemplateBuilder builder,
            @Value("${loki.url:http://loki:3100}") String lokiUrl) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        this.lokiUrl = lokiUrl;
    }

    /**
     * Lista logs aplicando filtros. Retorna formato simplificado pro front.
     *
     * @param service filtro exato pelo label `service` (ex: "iam-service"). Opcional.
     * @param level   filtro exato pelo label `level` (ERROR/WARN/INFO/DEBUG). Opcional.
     * @param search  texto a buscar no corpo da linha (LogQL |= "search"). Opcional.
     * @param sinceMinutes janela retroativa em minutos (default: 30).
     * @param limit numero maximo de linhas (default: 200, max: 1000).
     */
    @GetMapping
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<?> listLogs(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "30") int sinceMinutes,
            @RequestParam(defaultValue = "200") int limit) {

        int safeLimit = Math.min(Math.max(limit, 1), 1000);
        int safeSinceMinutes = Math.min(Math.max(sinceMinutes, 1), 1440);

        String query = buildLogQl(service, level, search);
        Instant end = Instant.now();
        Instant start = end.minus(Duration.ofMinutes(safeSinceMinutes));

        java.net.URI uri = UriComponentsBuilder.fromUriString(lokiUrl + "/loki/api/v1/query_range")
                .queryParam("query", query)
                .queryParam("start", toNanos(start))
                .queryParam("end", toNanos(end))
                .queryParam("limit", safeLimit)
                .queryParam("direction", "backward")
                .build()
                .encode()
                .toUri();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = restTemplate.getForObject(uri, Map.class);
            return ResponseEntity.ok(simplify(raw, safeLimit));
        } catch (RestClientException e) {
            log.warn("Loki query falhou: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "entries", List.of(),
                    "error", "Loki indisponivel: " + e.getMessage()
            ));
        }
    }

    /** Lista os servicos (labels) disponiveis para popular o dropdown. */
    @GetMapping("/services")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<?> listServices() {
        java.net.URI uri = java.net.URI.create(lokiUrl + "/loki/api/v1/label/service/values");
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = restTemplate.getForObject(uri, Map.class);
            Object data = raw != null ? raw.get("data") : null;
            return ResponseEntity.ok(Map.of("services", data != null ? data : List.of()));
        } catch (RestClientException e) {
            return ResponseEntity.ok(Map.of("services", List.of(), "error", e.getMessage()));
        }
    }

    // ==================== helpers ====================

    private String buildLogQl(String service, String level, String search) {
        List<String> selectors = new ArrayList<>();
        if (service != null && !service.isBlank()) {
            selectors.add("service=\"" + escape(service) + "\"");
        } else {
            selectors.add("service=~\".+\"");
        }
        if (level != null && !level.isBlank()) {
            selectors.add("level=\"" + escape(level.toUpperCase()) + "\"");
        }
        String logql = "{" + String.join(",", selectors) + "}";
        if (search != null && !search.isBlank()) {
            logql = logql + " |= \"" + escape(search) + "\"";
        }
        return logql;
    }

    private String escape(String v) {
        return v.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String toNanos(Instant i) {
        return Long.toString(i.getEpochSecond() * 1_000_000_000L + i.getNano());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> simplify(Map<String, Object> raw, int limit) {
        List<Map<String, Object>> entries = new ArrayList<>();
        if (raw == null) return Map.of("entries", entries);

        Map<String, Object> data = (Map<String, Object>) raw.getOrDefault("data", Map.of());
        List<Map<String, Object>> result = (List<Map<String, Object>>) data.getOrDefault("result", List.of());

        for (Map<String, Object> stream : result) {
            Map<String, String> labels = (Map<String, String>) stream.getOrDefault("stream", Map.of());
            List<List<String>> values = (List<List<String>>) stream.getOrDefault("values", List.of());
            String service = labels.getOrDefault("service", "?");
            String level = labels.getOrDefault("level", "");
            for (List<String> v : values) {
                if (v.size() < 2) continue;
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("timestamp", parseNanos(v.get(0)));
                entry.put("service", service);
                entry.put("level", level);
                entry.put("line", v.get(1));
                entries.add(entry);
            }
        }
        entries.sort((a, b) -> Long.compare(
                (long) b.getOrDefault("timestamp", 0L),
                (long) a.getOrDefault("timestamp", 0L)
        ));
        if (entries.size() > limit) entries = entries.subList(0, limit);
        return Map.of("entries", entries);
    }

    private long parseNanos(String ns) {
        try {
            return Long.parseLong(ns) / 1_000_000L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
