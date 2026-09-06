package br.com.uebiescola.core.infrastructure.audit;

import br.com.uebiescola.core.infrastructure.persistence.entity.AuditLogEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaAuditLogRepository;
import br.com.uebiescola.core.infrastructure.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final JpaAuditLogRepository auditLogRepository;

    private static final Map<String, String> ENTITY_NAMES = Map.of(
            "SchoolController", "Escola",
            "UserController", "Usuário"
    );

    @Pointcut("within(br.com.uebiescola.core.presentation.controller..*)")
    public void controllerMethods() {}

    @Pointcut("within(br.com.uebiescola.core.presentation.controller.SettingsController)")
    public void settingsController() {}

    @Pointcut("within(br.com.uebiescola.core.presentation.controller.AuditController)")
    public void auditController() {}

    @Pointcut("within(br.com.uebiescola.core.presentation.controller.SchoolStatsController)")
    public void statsController() {}

    @AfterReturning(pointcut = "controllerMethods() && !settingsController() && !auditController() && !statsController() && @annotation(org.springframework.web.bind.annotation.PostMapping)", returning = "result")
    public void auditPost(JoinPoint joinPoint, Object result) {
        audit(joinPoint, "Criou");
    }

    @AfterReturning(pointcut = "controllerMethods() && !settingsController() && !auditController() && !statsController() && @annotation(org.springframework.web.bind.annotation.PutMapping)", returning = "result")
    public void auditPut(JoinPoint joinPoint, Object result) {
        audit(joinPoint, "Editou");
    }

    @AfterReturning(pointcut = "controllerMethods() && !settingsController() && !auditController() && !statsController() && @annotation(org.springframework.web.bind.annotation.DeleteMapping)", returning = "result")
    public void auditDelete(JoinPoint joinPoint, Object result) {
        audit(joinPoint, "Excluiu");
    }

    @AfterReturning(pointcut = "controllerMethods() && !settingsController() && !auditController() && !statsController() && @annotation(org.springframework.web.bind.annotation.PatchMapping)", returning = "result")
    public void auditPatch(JoinPoint joinPoint, Object result) {
        audit(joinPoint, "Atualizou");
    }

    /**
     * A-5 AUDITORIAADMINPLATAFORMA 03/09/2026: registra leitura marcada com
     * @AuditableRead. Diferente das mutacoes acima, aqui NAO ignoramos CEO —
     * o proposito e justamente rastrear equipe UebiEscola abrindo dado de
     * escola-cliente. Aceita schoolId nulo (audit_log.school_id ficou
     * nullable em V29).
     */
    @AfterReturning(pointcut = "@annotation(auditableRead)", returning = "result")
    public void auditRead(JoinPoint joinPoint, AuditableRead auditableRead, Object result) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) return;

            // Extrai schoolId do path variable (se houver Long/UUID)
            Long targetSchoolId = extractTargetSchoolId(joinPoint.getArgs());

            String action = auditableRead.action() + " " + auditableRead.entity();
            String details = auditableRead.entity()
                    + (targetSchoolId != null ? " [schoolId=" + targetSchoolId + "]" : " [lista]");

            AuditLogEntity logEntity = AuditLogEntity.builder()
                    .schoolId(targetSchoolId != null ? targetSchoolId : user.getSchoolId())
                    .userEmail(user.getEmail())
                    .action(action)
                    .details(details)
                    .ipAddress(resolveClientIp())
                    .build();
            auditLogRepository.save(logEntity);
        } catch (Exception e) {
            log.warn("Falha ao registrar auditoria de leitura: {}", e.getMessage());
        }
    }

    /**
     * REVALIDACAO-ADMIN-PLATAFORMA 06/09/2026 (A-5): captura IP do request atual
     * pra preencher audit_logs.ip_address. Prefere X-Forwarded-For (o gateway
     * nginx seta com o IP real do cliente). Retorna null se nao ha request
     * (job scheduled, teste etc).
     */
    private String resolveClientIp() {
        try {
            var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (!(attrs instanceof org.springframework.web.context.request.ServletRequestAttributes servletAttrs)) return null;
            jakarta.servlet.http.HttpServletRequest req = servletAttrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
            String xreal = req.getHeader("X-Real-IP");
            if (xreal != null && !xreal.isBlank()) return xreal.trim();
            return req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private Long extractTargetSchoolId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long l) return l;
            // UUID de escola nao ajuda a preencher schoolId — devolve null.
        }
        return null;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void audit(JoinPoint joinPoint, String actionVerb) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) return;

            // CEO não tem schoolId — não registra audit para evitar constraint violation
            if (user.getSchoolId() == null) return;

            String className = joinPoint.getTarget().getClass().getSimpleName();
            String entityName = ENTITY_NAMES.getOrDefault(className, className.replace("Controller", ""));
            String methodName = joinPoint.getSignature().getName();

            String action = actionVerb + " " + entityName;
            String details = buildDetails(methodName, entityName, joinPoint.getArgs());

            AuditLogEntity logEntity = AuditLogEntity.builder()
                    .schoolId(user.getSchoolId())
                    .userEmail(user.getEmail())
                    .action(action)
                    .details(details)
                    .ipAddress(resolveClientIp())
                    .build();
            auditLogRepository.save(logEntity);

        } catch (Exception e) {
            log.warn("Falha ao registrar auditoria: {}", e.getMessage());
        }
    }

    private String buildDetails(String methodName, String entityName, Object[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append(entityName).append(" - ").append(methodName);

        for (Object arg : args) {
            if (arg instanceof Long || arg instanceof String) {
                sb.append(" [id=").append(arg).append("]");
                break;
            }
        }
        return sb.toString();
    }
}
