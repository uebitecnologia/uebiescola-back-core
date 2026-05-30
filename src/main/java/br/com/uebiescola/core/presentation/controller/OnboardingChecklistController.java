package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.infrastructure.client.AcademicOnboardingClient;
import br.com.uebiescola.core.infrastructure.client.CommunicationOnboardingClient;
import br.com.uebiescola.core.infrastructure.client.FinanceClient;
import br.com.uebiescola.core.infrastructure.client.PlansOnboardingClient;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolRepository;
import br.com.uebiescola.core.infrastructure.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checklist de onboarding pra escola recem-criada. Agrega checks de 5
 * services e devolve estado de 8 passos da primeira semana. Frontend
 * usa pra renderizar card de progresso na home do admin escola.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingChecklistController {

    private final JpaSchoolRepository schoolRepository;
    private final AcademicOnboardingClient academicOnboardingClient;
    private final FinanceClient financeClient;
    private final PlansOnboardingClient plansOnboardingClient;
    private final CommunicationOnboardingClient communicationOnboardingClient;

    @Value("${uebi.internal-token}")
    private String internalToken;

    @GetMapping("/checklist")
    public ResponseEntity<Map<String, Object>> getChecklist(
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest request) {

        if (user == null || user.getSchoolId() == null) {
            return ResponseEntity.badRequest().build();
        }

        Long schoolId = user.getSchoolId();
        String authHeader = request.getHeader("Authorization");

        // Cada step e independente — uma falha de Feign nao zera o checklist.
        boolean schoolComplete = isSchoolDataComplete(schoolId);
        boolean paymentConfigured = checkSilent(() -> plansOnboardingClient
                .hasPaymentConfigured(schoolId, internalToken)
                .getOrDefault("configured", false));

        // Counts academic num unico request (internal endpoint)
        Map<String, Long> academicCounts = checkSilentMap(() -> academicOnboardingClient
                .getOnboardingCounts(schoolId, internalToken));
        boolean hasClass = academicCounts.getOrDefault("classes", 0L) > 0L;
        boolean hasStudent = academicCounts.getOrDefault("students", 0L) > 0L;
        boolean hasTeacher = academicCounts.getOrDefault("teachers", 0L) > 0L;

        boolean hasGuardian = checkSilent(() -> !financeClient
                .getGuardians(authHeader, schoolId).isEmpty());
        boolean hasAnnouncement = checkSilent(() -> communicationOnboardingClient
                .announcementCount(schoolId, internalToken)
                .getOrDefault("count", 0L) > 0L);
        boolean hasInvoice = checkSilent(() -> !financeClient
                .getInvoices(authHeader, schoolId).isEmpty());

        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(step("school", "Completar dados da escola", schoolComplete, "/school"));
        steps.add(step("payment", "Configurar pagamento", paymentConfigured, "/settings"));
        steps.add(step("class", "Criar a primeira turma", hasClass, "/classes/new"));
        steps.add(step("guardian", "Cadastrar primeiro responsavel", hasGuardian, "/guardians/new"));
        steps.add(step("student", "Cadastrar primeiro aluno", hasStudent, "/students/new"));
        steps.add(step("teacher", "Convidar primeiro professor", hasTeacher, "/teachers/new"));
        steps.add(step("announcement", "Enviar primeiro comunicado", hasAnnouncement, "/communication/announcements"));
        steps.add(step("invoice", "Gerar a primeira mensalidade", hasInvoice, "/finance/invoices/new"));

        long completed = steps.stream().filter(s -> Boolean.TRUE.equals(s.get("done"))).count();
        int percent = (int) Math.round((completed * 100.0) / steps.size());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("schoolId", schoolId);
        response.put("steps", steps);
        response.put("completed", completed);
        response.put("total", steps.size());
        response.put("percent", percent);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> step(String key, String label, boolean done, String link) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("label", label);
        m.put("done", done);
        m.put("link", link);
        return m;
    }

    private boolean isSchoolDataComplete(Long schoolId) {
        SchoolEntity s = schoolRepository.findById(schoolId).orElse(null);
        if (s == null) return false;
        boolean hasDocument = (s.getCnpj() != null && !s.getCnpj().isBlank())
                || (s.getCpf() != null && !s.getCpf().isBlank());
        boolean hasLogo = s.getLogoBytes() != null && s.getLogoBytes().length > 0;
        boolean hasAddress = s.getAddress() != null
                && s.getAddress().getCity() != null
                && !s.getAddress().getCity().isBlank();
        return hasDocument && hasLogo && hasAddress;
    }

    private Map<String, Long> checkSilentMap(java.util.function.Supplier<Map<String, Long>> supplier) {
        try {
            Map<String, Long> result = supplier.get();
            return result != null ? result : Map.of();
        } catch (Exception e) {
            log.warn("[ONBOARDING] check map falhou: {}", e.getMessage());
            return Map.of();
        }
    }

    private boolean checkSilent(java.util.function.Supplier<Boolean> supplier) {
        try {
            Boolean result = supplier.get();
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("[ONBOARDING] check falhou: {}", e.getMessage());
            return false;
        }
    }
}
