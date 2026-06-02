package br.com.uebiescola.core.arch;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Anti-regressao das migracoes UUID (Ondas 1-5).
 *
 * Bugs recorrentes pos-Onda 5: Response DTO sem campo `uuid` → admin chamava
 * `/{undefined}` no path REST → 400 silencioso. Esse teste reflexiona todos
 * os Response records do pacote de DTOs e exige que exponham `UUID uuid`.
 */
class ResponseDtoUuidContractTest {

    private static final Set<String> EXEMPT_RESPONSES = Set.of(
            // Auth/registration — sem entidade propria persistida ao caller
            "AuthRefreshResponse",
            "SchoolRegistrationResponse",
            // Audit logs paginados — ja tem id Long no schema legacy
            "AuditLogResponseDTO",
            "AuditLogDTO",
            // Global setting: identificado por chave (string), nao por id
            "GlobalSettingDTO",
            // SchoolSettings: 1:1 com schoolId (sem entidade independente)
            "SchoolSettingsDTO"
    );

    @Test
    void everyResponseDtoMustExposeUuid() {
        var provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));

        List<String> offenders = new ArrayList<>();

        for (String basePackage : List.of(
                "br.com.uebiescola.core.presentation.dto"
        )) {
            for (var bean : provider.findCandidateComponents(basePackage)) {
                String className = bean.getBeanClassName();
                if (className == null) continue;
                String simpleName = className.substring(className.lastIndexOf('.') + 1);

                boolean isOutput = (simpleName.endsWith("Response") || simpleName.endsWith("DTO"))
                        && !simpleName.endsWith("Request")
                        && !simpleName.endsWith("RequestDTO");
                if (!isOutput) continue;
                if (simpleName.contains("$")) continue;
                if (EXEMPT_RESPONSES.contains(simpleName)) continue;

                Class<?> clazz;
                try {
                    clazz = Class.forName(className);
                } catch (ClassNotFoundException e) {
                    continue;
                }

                if (!hasUuidField(clazz)) {
                    offenders.add(simpleName);
                }
            }
        }

        assertThat(offenders)
                .as("DTOs sem campo UUID uuid (adicione `UUID uuid` no record ou liste em EXEMPT_RESPONSES com justificativa). "
                        + "Sem uuid o frontend acaba chamando endpoints /{undefined}.")
                .isEmpty();
    }

    private static boolean hasUuidField(Class<?> clazz) {
        // Aceita UUID ou String com nome "uuid" (alguns DTOs serializam UUID
        // como String pra facilitar consumo no front).
        if (clazz.isRecord()) {
            for (RecordComponent rc : clazz.getRecordComponents()) {
                if ("uuid".equals(rc.getName())
                        && (rc.getType() == UUID.class || rc.getType() == String.class)) return true;
            }
            return false;
        }
        for (var f : clazz.getDeclaredFields()) {
            if ("uuid".equals(f.getName())
                    && (f.getType() == UUID.class || f.getType() == String.class)) return true;
        }
        return false;
    }
}
