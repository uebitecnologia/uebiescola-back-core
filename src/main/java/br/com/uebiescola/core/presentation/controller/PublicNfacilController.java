package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.usecase.NfacilSignupUseCase;
import br.com.uebiescola.core.presentation.dto.NfacilSignupRequest;
import br.com.uebiescola.core.presentation.dto.NfacilSignupResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint publico de signup self-service do NF Facil — produto wedge
 * (NFS-e automatica pra escola PJ) com landing propria em nfacil.uebiescola.com.br.
 *
 * Diferente do /api/v1/public/register/self-service: nao pede senha (magic link),
 * exige CNPJ ativo (PJ), e cria escola com plano "NF Facil" ja configurado.
 */
@RestController
@RequestMapping("/api/v1/public/nfacil")
@RequiredArgsConstructor
@Slf4j
public class PublicNfacilController {

    private final NfacilSignupUseCase nfacilSignupUseCase;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid NfacilSignupRequest request) {
        try {
            NfacilSignupResponse response = nfacilSignupUseCase.execute(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            log.warn("[NFACIL-SIGNUP] violacao de constraint: {}",
                    e.getMostSpecificCause().getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Dados duplicados. CNPJ ou email ja cadastrado."));
        } catch (Exception e) {
            log.error("[NFACIL-SIGNUP] erro inesperado", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erro inesperado. Tente novamente em alguns minutos."));
        }
    }
}
