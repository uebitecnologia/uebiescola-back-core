package br.com.uebiescola.core.infrastructure.config;

import br.com.uebiescola.core.domain.context.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantAspect {

    @Autowired
    private EntityManager entityManager;

    @Before("execution(* br.com.uebiescola.core.infrastructure.persistence.repository.*.*(..))")
    public void beforeExecution() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
        }
    }
}