package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.service.CnpjLookupService;
import br.com.uebiescola.core.application.service.CpfAvailabilityService;
import br.com.uebiescola.core.presentation.dto.CnpjLookupResponse;
import br.com.uebiescola.core.presentation.dto.CpfAvailabilityResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints publicos (sem autenticacao) usados pelo wizard de signup e
 * pelo formulario de cadastro do admin pra validar e enriquecer CNPJ/CPF.
 *
 * CNPJ: BrasilAPI com cache Redis 30d.
 * CPF: apenas digitos verificadores + checagem de duplicidade local.
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Slf4j
public class CnpjLookupController {

    private final CnpjLookupService cnpjLookupService;
    private final CpfAvailabilityService cpfAvailabilityService;

    @GetMapping("/cnpj/{cnpj}/lookup")
    public ResponseEntity<CnpjLookupResponse> lookupCnpj(@PathVariable String cnpj) {
        return ResponseEntity.ok(cnpjLookupService.lookup(cnpj));
    }

    @GetMapping("/cpf/{cpf}/availability")
    public ResponseEntity<CpfAvailabilityResponse> checkCpf(@PathVariable String cpf) {
        return ResponseEntity.ok(cpfAvailabilityService.check(cpf));
    }
}
