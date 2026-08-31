package br.com.uebiescola.core.arch;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API-2 (rodada 6 pente-fino 31/08): replica do arch test do academic.
 * Publicas: /public, /schools/tenant, /schools/public, /internal.
 */
class RoleAuthorizationArchTest {

    private static final String CONTROLLER_PKG = "br.com.uebiescola.core.presentation.controller";

    private static final List<String> ROUTE_ALLOWLIST_PREFIXES = Arrays.asList(
            "/api/v1/public",
            "/api/v1/schools/tenant",
            "/api/v1/schools/public",
            "/api/v1/internal",
            "/api/v1/core/swagger",
            "/api/v1/core/api-docs"
    );

    private static final List<Class<? extends Annotation>> HTTP_MAPPINGS = Arrays.asList(
            GetMapping.class, PostMapping.class, PutMapping.class,
            PatchMapping.class, DeleteMapping.class, RequestMapping.class
    );

    @Test
    void every_endpoint_must_have_preauthorize_or_be_in_allowlist() throws Exception {
        List<String> unprotected = new ArrayList<>();

        for (Class<?> controller : findControllers()) {
            boolean classHasPreAuth = controller.isAnnotationPresent(PreAuthorize.class);
            String classPath = extractClassPath(controller);

            for (Method method : controller.getDeclaredMethods()) {
                String methodPath = extractMethodPath(method);
                if (methodPath == null) continue;

                String httpPath = classPath + methodPath;
                if (isInAllowlist(httpPath)) continue;
                if (classHasPreAuth) continue;
                if (method.isAnnotationPresent(PreAuthorize.class)) continue;

                unprotected.add(String.format("%s.%s → %s",
                        controller.getSimpleName(), method.getName(), httpPath));
            }
        }

        assertThat(unprotected)
                .as("Endpoints sem @PreAuthorize e fora da allowlist — ADICIONE "
                        + "hasAnyRole(...) ou mova a rota pra /public/**|/internal/**.")
                .isEmpty();
    }

    private List<Class<?>> findControllers() throws Exception {
        var provider = new org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new org.springframework.core.type.filter.AnnotationTypeFilter(RestController.class));
        return provider.findCandidateComponents(CONTROLLER_PKG).stream()
                .map(bd -> {
                    try { return Class.forName(bd.getBeanClassName()); }
                    catch (ClassNotFoundException e) { throw new RuntimeException(e); }
                })
                .collect(Collectors.toList());
    }

    private String extractClassPath(Class<?> controller) {
        RequestMapping cls = controller.getAnnotation(RequestMapping.class);
        if (cls == null) return "";
        return cls.value().length > 0 ? cls.value()[0]
                : (cls.path().length > 0 ? cls.path()[0] : "");
    }

    private String extractMethodPath(Method method) {
        for (Class<? extends Annotation> anno : HTTP_MAPPINGS) {
            Annotation a = method.getAnnotation(anno);
            if (a == null) continue;
            try {
                String[] value = (String[]) anno.getMethod("value").invoke(a);
                if (value.length > 0) return value[0];
                String[] path = (String[]) anno.getMethod("path").invoke(a);
                if (path.length > 0) return path[0];
                return "";
            } catch (Exception e) { return ""; }
        }
        return null;
    }

    private boolean isInAllowlist(String path) {
        return ROUTE_ALLOWLIST_PREFIXES.stream()
                .anyMatch(p -> path.equals(p) || path.startsWith(p + "/"));
    }
}
