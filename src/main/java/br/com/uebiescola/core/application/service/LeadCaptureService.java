package br.com.uebiescola.core.application.service;

import br.com.uebiescola.core.infrastructure.persistence.entity.LeadEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaLeadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Captura leads vindos de material gratuito (lead-magnets) e formularios
 * publicos do site marketing. Idempotente: o mesmo email + recurso pode
 * gravar varias linhas (intencional — interesse repetido conta).
 *
 * Nao envia email aqui — apenas registra. Disparo de email com link de
 * download pode ser tratado por um scheduler ou consumer futuramente.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LeadCaptureService {

    private static final Pattern EMAIL_RX = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final JpaLeadRepository leadRepository;

    @Transactional
    public LeadEntity capture(String email, String name, String resource, String source,
                              String userAgent, String ipAddress) {
        String cleanEmail = sanitizeEmail(email);
        if (cleanEmail == null) {
            throw new IllegalArgumentException("E-mail invalido");
        }
        if (resource == null || resource.isBlank()) {
            throw new IllegalArgumentException("Recurso ausente");
        }
        LeadEntity lead = LeadEntity.builder()
                .email(cleanEmail)
                .name(trim(name, 160))
                .resource(trim(resource, 80))
                .source(trim(source, 80))
                .userAgent(trim(userAgent, 500))
                .ipAddress(trim(ipAddress, 64))
                .build();
        LeadEntity saved = leadRepository.save(lead);
        long historic = leadRepository.countByEmailAndResource(cleanEmail, lead.getResource());
        log.info("[LEAD] Capturado | email={} resource={} source={} historico={}",
                cleanEmail, lead.getResource(), lead.getSource(), historic);
        return saved;
    }

    private String sanitizeEmail(String email) {
        if (email == null) return null;
        String trimmed = email.trim().toLowerCase();
        if (trimmed.length() > 255) return null;
        return EMAIL_RX.matcher(trimmed).matches() ? trimmed : null;
    }

    private String trim(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return t.length() > max ? t.substring(0, max) : t;
    }
}
