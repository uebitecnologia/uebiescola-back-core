package br.com.uebiescola.core.application.service;

import br.com.uebiescola.core.application.dto.BrasilApiCnpjResponse;
import br.com.uebiescola.core.infrastructure.config.CacheConfig;
import br.com.uebiescola.core.infrastructure.external.BrasilApiClient;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolRepository;
import br.com.uebiescola.core.presentation.dto.CnpjLookupResponse;
import br.com.uebiescola.core.presentation.validation.CnpjValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Consulta CNPJ na BrasilAPI com cache Redis (30d) e enriquece a resposta com:
 * - validacao de digitos verificadores
 * - flag de situacao ATIVA
 * - flag de duplicidade (CNPJ ja cadastrado em schools)
 *
 * Endpoint publico /api/v1/public/cnpj/{cnpj}/lookup usa este service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CnpjLookupService {

    private static final String SITUACAO_ATIVA = "ATIVA";

    private final BrasilApiClient brasilApiClient;
    private final JpaSchoolRepository schoolRepository;
    private final CnpjValidator cnpjValidator = new CnpjValidator();

    public CnpjLookupResponse lookup(String rawCnpj) {
        String digits = rawCnpj == null ? "" : rawCnpj.replaceAll("\\D", "");

        if (digits.length() != 14 || !validateDigits(digits)) {
            return CnpjLookupResponse.invalid("CNPJ invalido. Verifique os digitos e tente novamente.");
        }

        boolean alreadyRegistered = schoolRepository.existsByCnpj(digits);

        Optional<BrasilApiCnpjResponse> remote = fetchCached(digits);
        if (remote.isEmpty()) {
            return CnpjLookupResponse.notFound(digits, alreadyRegistered);
        }

        BrasilApiCnpjResponse data = remote.get();
        boolean ativa = SITUACAO_ATIVA.equalsIgnoreCase(data.getSituacaoCadastral());

        return CnpjLookupResponse.ok(digits, data, ativa, alreadyRegistered);
    }

    /**
     * Layer separada pra que @Cacheable funcione (self-invocation bypassa proxy).
     * Cache so e populado quando a API responde 200 — falhas/404 nao sao cacheadas.
     */
    @Cacheable(value = CacheConfig.CACHE_BRASILAPI_CNPJ, key = "#digits", unless = "#result == null or !#result.isPresent()")
    public Optional<BrasilApiCnpjResponse> fetchCached(String digits) {
        return brasilApiClient.fetchCnpj(digits);
    }

    private boolean validateDigits(String digits) {
        return cnpjValidator.isValid(digits, null);
    }
}
