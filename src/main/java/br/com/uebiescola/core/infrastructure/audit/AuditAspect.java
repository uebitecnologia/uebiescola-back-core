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
