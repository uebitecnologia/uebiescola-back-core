package br.com.uebiescola.core.infrastructure.security;

import br.com.uebiescola.core.domain.context.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Value("${uebi.jwt.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoverToken(request);

        // 1. Capturamos o Header que o CEO envia ao clicar em "Acessar Portal"
        String xSchoolId = request.getHeader("X-School-Id");

        if (token != null) {
            try {
                Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String email = claims.getSubject();
                String role = claims.get("role", String.class);
                String externalId = claims.get("userExternalId", String.class);

                // 2. Captura robusta do schoolId do Token
                Long schoolId = null;
                if (claims.get("schoolId") != null) {
                    schoolId = ((Number) claims.get("schoolId")).longValue();
                }

                // 3. LÓGICA DE SUPORTE CEO:
                // Se o schoolId no token for null e for CEO, tentamos pegar do Header X-School-Id
                if (schoolId == null && "ROLE_CEO".equals(role) && xSchoolId != null) {
                    try {
                        schoolId = Long.parseLong(xSchoolId);
                        logger.info("[ACADEMIC] Modo Suporte: CEO acessando escola " + schoolId);
                    } catch (NumberFormatException e) {
                        logger.error("[ACADEMIC] Header X-School-Id invalido");
                    }
                }

                // 4. IMPORTANTE: Seta o TenantContext para as queries SQL funcionarem
                if (schoolId != null) {
                    // Verifique se você importou o TenantContext correto
                    TenantContext.setCurrentTenant(schoolId);
                }

                AuthenticatedUser userPrincipal = new AuthenticatedUser(
                        email,
                        role,
                        schoolId,
                        externalId
                );

                var authorities = List.of(new SimpleGrantedAuthority(role));

                var authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                logger.error("Falha ao validar Token JWT: " + e.getMessage());
                // Em caso de erro de assinatura, limpamos o contexto por segurança
                SecurityContextHolder.clearContext();
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 5. Limpa o TenantContext após a requisição para não vazar dados entre threads
            TenantContext.clear();
        }
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return authHeader.replace("Bearer ", "");
    }
}