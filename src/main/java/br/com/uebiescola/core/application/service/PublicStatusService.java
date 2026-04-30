package br.com.uebiescola.core.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico que monta o snapshot publico de status (estilo Atlassian Statuspage).
 *
 * Le up{job=uebiescola-services} do Prometheus e agrupa por componente
 * que faz sentido pro cliente (Plataforma, Academico, Financeiro, etc).
 *
 * Tambem le incidents.json de um arquivo bind-mountado para listar
 * incidentes passados / proxima manutencao.
 *
 * Resultado em cache por 30s.
 */
@Service
@Slf4j
public class PublicStatusService {

    private static final ZoneId BRT = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final long CACHE_MS = 30_000L;
    private static final int HISTORY_DAYS = 90;

    // Status string constants — bate com CSS no frontend
    private static final String OPERATIONAL = "operational";
    private static final String DEGRADED = "degraded";
    private static final String PARTIAL_OUTAGE = "partial_outage";
    private static final String MAJOR_OUTAGE = "major_outage";
    private static final String MAINTENANCE = "maintenance";
    private static final String NO_DATA = "no_data";

    // Componentes visiveis ao publico -> servicos internos do Prometheus
    private static final List<Component> COMPONENTS = List.of(
            new Component("Plataforma e login", List.of("iam", "core")),
            new Component("Acadêmico e Matrículas", List.of("academic", "enrollment")),
            new Component("Financeiro e Pagamentos", List.of("finance", "plans")),
            new Component("Notificações (e-mail, push, WhatsApp)", List.of("notification")),
            new Component("Comunicação escola-família", List.of("communication", "support"))
    );

    private final RestTemplate restTemplate;
    private final String prometheusUrl;
    private final String incidentsPath;
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public PublicStatusService(
            RestTemplateBuilder builder,
            @Value("${prometheus.url:http://prometheus:9090}") String prometheusUrl,
            @Value("${public-status.incidents-path:/app/data/incidents.json}") String incidentsPath) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(8))
                .build();
        this.prometheusUrl = prometheusUrl;
        this.incidentsPath = incidentsPath;
    }

    @PostConstruct
    void logInit() {
        log.info("[PUBLIC-STATUS] Servico inicializado | prometheus={} | incidents={}",
                prometheusUrl, incidentsPath);
    }

    /** Snapshot completo (cacheado 30s). */
    public Map<String, Object> getSnapshot() {
        CacheEntry entry = cache.get("snapshot");
        if (entry != null && entry.isFresh()) {
            return entry.payload;
        }
        Map<String, Object> snapshot = buildSnapshot();
        cache.put("snapshot", new CacheEntry(snapshot, System.currentTimeMillis()));
        return snapshot;
    }

    private Map<String, Object> buildSnapshot() {
        Map<String, Map<String, Integer>> dailyHistory = queryDailyHistory();
        Map<String, Integer> currentUp = queryCurrentUp();

        List<Map<String, Object>> componentList = new ArrayList<>();
        boolean anyDown = false;
        boolean anyDegraded = false;
        for (Component c : COMPONENTS) {
            String currentStatus = computeCurrent(c, currentUp);
            if (MAJOR_OUTAGE.equals(currentStatus) || PARTIAL_OUTAGE.equals(currentStatus)) {
                anyDown = true;
            } else if (DEGRADED.equals(currentStatus)) {
                anyDegraded = true;
            }

            Map<String, Object> compMap = new LinkedHashMap<>();
            compMap.put("name", c.publicName);
            compMap.put("status", currentStatus);
            compMap.put("history", buildHistory(c, dailyHistory));
            componentList.add(compMap);
        }

        IncidentsBundle bundle = readIncidents();

        // Status geral
        String overall = OPERATIONAL;
        if (bundle.activeMaintenance) overall = MAINTENANCE;
        else if (anyDown) overall = MAJOR_OUTAGE;
        else if (anyDegraded) overall = DEGRADED;

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("overall", overall);
        snapshot.put("components", componentList);
        snapshot.put("incidents", bundle.incidents);
        snapshot.put("nextMaintenance", bundle.nextMaintenance);
        snapshot.put("updatedAt", Instant.now().toString());
        snapshot.put("historyDays", HISTORY_DAYS);
        return snapshot;
    }

    private String computeCurrent(Component c, Map<String, Integer> currentUp) {
        int upCount = 0, total = c.services.size();
        for (String svc : c.services) {
            Integer v = currentUp.get(svc);
            if (v == null) continue;
            if (v == 1) upCount++;
        }
        if (upCount == total) return OPERATIONAL;
        if (upCount == 0) return MAJOR_OUTAGE;
        return PARTIAL_OUTAGE;
    }

    private List<Map<String, Object>> buildHistory(Component c, Map<String, Map<String, Integer>> dailyHistory) {
        LocalDate today = LocalDate.now(BRT);
        List<Map<String, Object>> out = new ArrayList<>(HISTORY_DAYS);
        for (int i = HISTORY_DAYS - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String key = d.format(ISO_DATE);
            int upCount = 0, samplesAvailable = 0;
            for (String svc : c.services) {
                Map<String, Integer> svcSeries = dailyHistory.get(svc);
                if (svcSeries == null) continue;
                Integer val = svcSeries.get(key);
                if (val == null) continue;
                samplesAvailable++;
                if (val == 1) upCount++;
            }
            String dayStatus;
            if (samplesAvailable == 0) {
                dayStatus = NO_DATA;
            } else if (upCount == samplesAvailable) {
                dayStatus = OPERATIONAL;
            } else if (upCount == 0) {
                dayStatus = MAJOR_OUTAGE;
            } else {
                dayStatus = PARTIAL_OUTAGE;
            }
            Map<String, Object> dayMap = new LinkedHashMap<>();
            dayMap.put("date", key);
            dayMap.put("status", dayStatus);
            out.add(dayMap);
        }
        return out;
    }

    /** up{} agora (instant query) -> map<service, 0|1>. */
    private Map<String, Integer> queryCurrentUp() {
        Map<String, Integer> out = new HashMap<>();
        try {
            String query = URLEncoder.encode("up{job=\"uebiescola-services\"}", StandardCharsets.UTF_8);
            URI url = URI.create(prometheusUrl + "/api/v1/query?query=" + query);
            String body = restTemplate.getForObject(url, String.class);
            if (body == null) return out;
            JsonNode root = mapper.readTree(body);
            JsonNode results = root.path("data").path("result");
            if (!results.isArray()) return out;
            for (JsonNode r : results) {
                String svc = r.path("metric").path("service").asText();
                if (svc.isEmpty()) continue;
                JsonNode val = r.path("value");
                if (!val.isArray() || val.size() < 2) continue;
                int v = "1".equals(val.get(1).asText()) ? 1 : 0;
                out.put(svc, v);
            }
        } catch (RestClientException | java.io.IOException e) {
            log.warn("[PUBLIC-STATUS] Falha ao consultar Prometheus (current): {}", e.getMessage());
        }
        return out;
    }

    /**
     * min_over_time(up[1d]) bucketizado por dia para os ultimos 90 dias.
     * Retorno: map<service, map<dateISO, 0|1>>.
     */
    private Map<String, Map<String, Integer>> queryDailyHistory() {
        Map<String, Map<String, Integer>> out = new HashMap<>();
        try {
            long now = Instant.now().getEpochSecond();
            long start = now - (HISTORY_DAYS * 24L * 3600L);
            int step = 24 * 3600; // 1d
            String query = URLEncoder.encode("min_over_time(up{job=\"uebiescola-services\"}[1d])", StandardCharsets.UTF_8);
            URI url = URI.create(String.format("%s/api/v1/query_range?query=%s&start=%d&end=%d&step=%d",
                    prometheusUrl, query, start, now, step));
            String body = restTemplate.getForObject(url, String.class);
            if (body == null) return out;
            JsonNode root = mapper.readTree(body);
            JsonNode results = root.path("data").path("result");
            if (!results.isArray()) return out;
            for (JsonNode r : results) {
                String svc = r.path("metric").path("service").asText();
                if (svc.isEmpty()) continue;
                Map<String, Integer> series = out.computeIfAbsent(svc, k -> new HashMap<>());
                JsonNode values = r.path("values");
                if (!values.isArray()) continue;
                for (JsonNode pair : values) {
                    if (!pair.isArray() || pair.size() < 2) continue;
                    long ts = pair.get(0).asLong();
                    String date = LocalDate.ofInstant(Instant.ofEpochSecond(ts), BRT).format(ISO_DATE);
                    int v = "1".equals(pair.get(1).asText()) ? 1 : 0;
                    series.merge(date, v, Math::min); // se houver 2 amostras no mesmo dia, fica o pior
                }
            }
        } catch (RestClientException | java.io.IOException e) {
            log.warn("[PUBLIC-STATUS] Falha ao consultar Prometheus (history): {}", e.getMessage());
        }
        return out;
    }

    private IncidentsBundle readIncidents() {
        IncidentsBundle bundle = new IncidentsBundle();
        File f = new File(incidentsPath);
        if (!f.exists() || !f.canRead()) {
            return bundle;
        }
        try {
            JsonNode root = mapper.readTree(f);
            JsonNode incidents = root.path("incidents");
            if (incidents.isArray()) {
                List<Map<String, Object>> list = new ArrayList<>();
                for (JsonNode n : incidents) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("date", n.path("date").asText());
                    m.put("title", n.path("title").asText());
                    m.put("status", n.path("status").asText("resolved"));
                    m.put("body", n.path("body").asText(""));
                    list.add(m);
                }
                // Mais recentes primeiro
                list.sort((a, b) -> ((String) b.get("date")).compareTo((String) a.get("date")));
                bundle.incidents = list;
            }
            JsonNode maintenance = root.path("nextMaintenance");
            if (!maintenance.isMissingNode() && !maintenance.isNull()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("when", maintenance.path("when").asText());
                m.put("description", maintenance.path("description").asText(""));
                bundle.nextMaintenance = m;
                bundle.activeMaintenance = maintenance.path("active").asBoolean(false);
            }
        } catch (Exception e) {
            log.warn("[PUBLIC-STATUS] Falha ao ler incidents.json: {}", e.getMessage());
        }
        return bundle;
    }

    private static final class Component {
        final String publicName;
        final List<String> services;
        Component(String publicName, List<String> services) {
            this.publicName = publicName;
            this.services = services;
        }
    }

    private static final class CacheEntry {
        final Map<String, Object> payload;
        final long timestamp;
        CacheEntry(Map<String, Object> payload, long timestamp) {
            this.payload = payload;
            this.timestamp = timestamp;
        }
        boolean isFresh() {
            return System.currentTimeMillis() - timestamp < CACHE_MS;
        }
    }

    private static final class IncidentsBundle {
        List<Map<String, Object>> incidents = Collections.emptyList();
        Map<String, Object> nextMaintenance = null;
        boolean activeMaintenance = false;
    }
}
