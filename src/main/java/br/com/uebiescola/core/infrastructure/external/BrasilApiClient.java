package br.com.uebiescola.core.infrastructure.external;

import br.com.uebiescola.core.application.dto.BrasilApiCnpjResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * Cliente HTTP pra BrasilAPI (CNPJ v1). Doc: https://brasilapi.com.br/docs#tag/CNPJ
 *
 * Retorna {@link Optional#empty()} quando o CNPJ nao existe (404) ou em falha de rede.
 * Quem chama deve diferenciar "nao encontrado" de "indisponivel" — hoje os dois caem
 * no mesmo bucket. Se o cache da camada acima ja tem valor, ele eh devolvido.
 */
@Component
@Slf4j
public class BrasilApiClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final ObjectMapper mapper = new ObjectMapper();

    public BrasilApiClient(
            RestTemplateBuilder builder,
            @Value("${brasilapi.cnpj-base-url:https://brasilapi.com.br/api/cnpj/v1}") String baseUrl) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(8))
                .build();
        this.baseUrl = baseUrl;
    }

    public Optional<BrasilApiCnpjResponse> fetchCnpj(String digits) {
        try {
            URI url = URI.create(baseUrl + "/" + digits);
            String body = restTemplate.getForObject(url, String.class);
            if (body == null) return Optional.empty();
            return Optional.of(parse(body));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            log.warn("[BRASILAPI] HTTP {} ao consultar CNPJ {}: {}", e.getStatusCode(), digits, e.getMessage());
            return Optional.empty();
        } catch (RestClientException | java.io.IOException e) {
            log.warn("[BRASILAPI] Falha ao consultar CNPJ {}: {}", digits, e.getMessage());
            return Optional.empty();
        }
    }

    private BrasilApiCnpjResponse parse(String body) throws java.io.IOException {
        JsonNode n = mapper.readTree(body);
        BrasilApiCnpjResponse r = new BrasilApiCnpjResponse();
        r.setCnpj(n.path("cnpj").asText(null));
        r.setRazaoSocial(n.path("razao_social").asText(null));
        r.setNomeFantasia(n.path("nome_fantasia").asText(null));
        r.setSituacaoCadastral(n.path("descricao_situacao_cadastral").asText(null));
        r.setLogradouro(n.path("logradouro").asText(null));
        r.setNumero(n.path("numero").asText(null));
        r.setComplemento(n.path("complemento").asText(null));
        r.setBairro(n.path("bairro").asText(null));
        r.setMunicipio(n.path("municipio").asText(null));
        r.setUf(n.path("uf").asText(null));
        r.setCep(n.path("cep").asText(null));
        r.setEmail(n.path("email").asText(null));
        r.setTelefone(n.path("ddd_telefone_1").asText(null));
        r.setDataAbertura(n.path("data_inicio_atividade").asText(null));
        r.setCnaeFiscal(n.path("cnae_fiscal_descricao").asText(null));
        return r;
    }
}
