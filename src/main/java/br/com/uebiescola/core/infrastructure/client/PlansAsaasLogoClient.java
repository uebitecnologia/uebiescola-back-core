package br.com.uebiescola.core.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Wrapper minimo pra propagar logo da escola pra subconta Asaas (multipart).
 * Endpoint do plans-service: POST /api/v1/plans/internal/asaas/subaccount/{id}/logo
 * Autenticado via header X-Internal-Token.
 *
 * Erros sao silenciosos (log warn) pra nao bloquear o fluxo principal de
 * upload de logo da escola no core — se subconta nao existir ou Asaas
 * estiver fora, a escola ainda atualiza seu logo localmente.
 */
@Component
public class PlansAsaasLogoClient {

    private static final Logger log = LoggerFactory.getLogger(PlansAsaasLogoClient.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl;
    private final String internalToken;

    public PlansAsaasLogoClient(
            @Value("${feign.plans-service.url:http://plans-service:8082/api/v1}") String baseUrl,
            @Value("${uebi.internal-token:}") String internalToken
    ) {
        this.baseUrl = baseUrl;
        this.internalToken = internalToken;
    }

    public boolean isConfigured() {
        return internalToken != null && !internalToken.isBlank();
    }

    /** Propaga logo pra subconta Asaas da escola. Best-effort. */
    public boolean propagateToAsaas(Long schoolId, byte[] fileBytes, String contentType, String fileName) {
        if (!isConfigured()) {
            log.debug("[PLANS-LOGO] uebi.internal-token nao configurado — pulando propagacao");
            return false;
        }
        if (schoolId == null || fileBytes == null || fileBytes.length == 0) return false;

        try {
            LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.parseMediaType(
                    contentType != null ? contentType : "image/png"));
            ByteArrayResource resource = new ByteArrayResource(fileBytes) {
                @Override public String getFilename() { return fileName != null ? fileName : "logo.png"; }
            };
            body.add("file", new HttpEntity<>(resource, fileHeaders));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-Internal-Token", internalToken);

            String url = baseUrl + "/plans/internal/asaas/subaccount/" + schoolId + "/logo";
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (resp.getStatusCode().is2xxSuccessful()) {
                Object synced = resp.getBody() != null ? resp.getBody().get("synced") : null;
                log.info("[PLANS-LOGO] Logo propagado pra Asaas — escola={} synced={}", schoolId, synced);
                return Boolean.TRUE.equals(synced);
            }
            return false;
        } catch (Exception e) {
            log.warn("[PLANS-LOGO] Falha ao propagar logo escola={}: {}", schoolId, e.getMessage());
            return false;
        }
    }
}
