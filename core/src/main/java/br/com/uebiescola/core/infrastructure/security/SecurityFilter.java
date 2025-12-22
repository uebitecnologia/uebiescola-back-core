package br.com.uebiescola.core.infrastructure.security;

import br.com.uebiescola.core.domain.context.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Value("${uebi.jwt.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = recoverToken(request);

        if (token != null && tokenService.isTokenValid(token)) {
            // Extrai dados do Token usando a chave secreta do YML
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secret.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 1. Extrair a Role (ex: ROLE_CEO) para resolver o 403 Forbidden
            String role = claims.get("role", String.class);
            List<SimpleGrantedAuthority> authorities = Collections.emptyList();

            if (role != null) {
                authorities = Collections.singletonList(new SimpleGrantedAuthority(role));
            }

            // 2. Setamos o TenantContext (Multi-tenancy)
            Long schoolId = claims.get("schoolId", Long.class);
            if (schoolId != null) {
                TenantContext.setCurrentTenant(schoolId);
            }

            // 3. Autentica no Spring Security passando as Authorities (Roles)
            var authentication = new UsernamePasswordAuthenticationToken(
                    claims.getSubject(),
                    null,
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.replace("Bearer ", "");
    }
}