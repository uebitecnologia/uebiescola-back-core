package br.com.uebiescola.core.infrastructure.security;

import br.com.uebiescola.core.domain.model.School;
import br.com.uebiescola.core.domain.repository.SchoolRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Resolve qual escola (schoolId) o request está mirando, em ordem de prioridade:
 *
 * 1. user.schoolId — se o token tem escola fixa (ADMIN, TEACHER, GUARDIAN)
 * 2. query param explícito — quando o CEO chama /endpoint?schoolId=X
 * 3. header X-School-Id — quando o gateway/cliente injeta
 * 4. subdomain do Host — escolamodelo.uebiescola.com.br → busca school por subdomain
 *
 * Subdomínios de sistema (api/admin/portal/www/status) não viram lookup de escola.
 * Retorna null quando nada bate — controller decide se responde 400.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantResolver {

    private static final Set<String> SYSTEM_SUBDOMAINS = Set.of(
            "api", "admin", "portal", "www", "status", "grafana", "rabbitmq"
    );

    private final SchoolRepository schoolRepository;

    public Long resolve(AuthenticatedUser user, Long requestSchoolId, HttpServletRequest request) {
        // 1. Token tem escola fixa
        if (user != null && user.getSchoolId() != null) {
            return user.getSchoolId();
        }
        // 2. Query/body explícito
        if (requestSchoolId != null) {
            return requestSchoolId;
        }
        if (request == null) {
            return null;
        }
        // 3. Header X-School-Id (gateway/cliente)
        String headerSchoolId = request.getHeader("X-School-Id");
        if (headerSchoolId != null && !headerSchoolId.isBlank()) {
            try {
                return Long.parseLong(headerSchoolId.trim());
            } catch (NumberFormatException ignored) {
                // segue pro próximo fallback
            }
        }
        // 4. Subdomain do Host
        String host = request.getHeader("Host");
        if (host == null || host.isBlank()) {
            host = request.getServerName();
        }
        String subdomain = extractSubdomain(host);
        if (subdomain == null) {
            return null;
        }
        return schoolRepository.findBySubdomain(subdomain)
                .map(School::getId)
                .orElseGet(() -> {
                    log.debug("Subdomain '{}' não corresponde a nenhuma escola.", subdomain);
                    return null;
                });
    }

    private static String extractSubdomain(String host) {
        if (host == null || host.isBlank()) return null;
        // Strip port se houver
        int colonIdx = host.indexOf(':');
        if (colonIdx > 0) host = host.substring(0, colonIdx);
        String[] parts = host.split("\\.");
        if (parts.length < 3) return null; // ex: localhost ou uebiescola.com.br (sem subdomain)
        String sub = parts[0].toLowerCase();
        if (SYSTEM_SUBDOMAINS.contains(sub)) return null;
        return sub;
    }
}
