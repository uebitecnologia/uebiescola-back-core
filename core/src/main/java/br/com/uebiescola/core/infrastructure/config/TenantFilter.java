package br.com.uebiescola.core.infrastructure.config;

import br.com.uebiescola.core.domain.context.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class TenantFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String tenantId = req.getHeader("X-Tenant-ID");

        if (tenantId != null && !tenantId.isEmpty()) {
            TenantContext.setCurrentTenant(Long.parseLong(tenantId));
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // Limpa o contexto após a requisição para evitar vazamento de dados entre Threads
            TenantContext.clear();
        }
    }
}