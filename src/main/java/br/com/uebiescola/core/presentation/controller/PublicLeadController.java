package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.service.LeadCaptureService;
import br.com.uebiescola.core.presentation.dto.LeadCaptureRequest;
import br.com.uebiescola.core.presentation.dto.LeadCaptureResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint publico para captura de leads vindos do site marketing
 * (Mapa da Inadimplencia, Pesquisa Perfil do Professor, etc).
 *
 * Aceita POST { email, name?, resource, source? }.
 * Resposta sempre 200 com flag `ok` — frontend usa pra mostrar download
 * mesmo em caso de validation error (UX nao bloqueia o lead).
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Slf4j
public class PublicLeadController {

    private final LeadCaptureService leadCaptureService;

    @PostMapping("/leads")
    public ResponseEntity<LeadCaptureResponse> captureLead(@RequestBody LeadCaptureRequest req,
                                                            HttpServletRequest http) {
        try {
            String userAgent = http.getHeader("User-Agent");
            String ip = extractClientIp(http);
            leadCaptureService.capture(req.getEmail(), req.getName(), req.getResource(),
                    req.getSource(), userAgent, ip);
            return ResponseEntity.ok(new LeadCaptureResponse(true, null));
        } catch (IllegalArgumentException e) {
            log.info("[LEAD] Rejeitado: {} (email={}, resource={})",
                    e.getMessage(), req.getEmail(), req.getResource());
            return ResponseEntity.ok(new LeadCaptureResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("[LEAD] Erro inesperado capturando lead: {}", e.getMessage(), e);
            return ResponseEntity.ok(new LeadCaptureResponse(false, "Erro interno — material liberado mesmo assim"));
        }
    }

    private String extractClientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return http.getRemoteAddr();
    }
}
