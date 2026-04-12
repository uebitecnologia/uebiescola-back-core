package br.com.uebiescola.core.infrastructure.config;

import br.com.uebiescola.core.domain.context.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantAspect {

    @Autowired
    private EntityManager entityManager;

    @Before("execution(* br.com.uebiescola.core.infrastructure.persistence.adapter.*.*(..))")
    public void beforeExecution() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()) {
            // 1. Verifica se o usuário tem a role de CEO
            boolean isCeo = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_CEO"::equals);

            if (isCeo) {
                // Se for CEO, desabilita o filtro para que ele veja todos os dados
                Session session = entityManager.unwrap(Session.class);
                session.disableFilter("tenantFilter");
                return;
            }

            // 2. Para os demais usuários, aplica o filtro de tenant
            // A lógica para obter o tenantId do token JWT deve estar em outro lugar
            // (provavelmente em um filtro que popula o TenantContext).
            // Aqui, apenas lemos o valor que já deve estar no contexto.
            Long tenantId = TenantContext.getCurrentTenant();
            if (tenantId != null) {
                Session session = entityManager.unwrap(Session.class);
                session.enableFilter("tenantFilter").setParameter("schoolId", tenantId);
            }
        }
    }
}
