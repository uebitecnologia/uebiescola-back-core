package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.service.TokenService;
import br.com.uebiescola.core.infrastructure.security.SecurityFilter;
import br.com.uebiescola.core.infrastructure.persistence.entity.AuditLogEntity;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolEntity;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolSettingsEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaAuditLogRepository;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolRepository;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolSettingsRepository;
import br.com.uebiescola.core.infrastructure.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private SecurityFilter securityFilter;

    @MockBean
    private JpaSchoolSettingsRepository settingsRepository;

    @MockBean
    private JpaAuditLogRepository auditLogRepository;

    @MockBean
    private JpaSchoolRepository schoolRepository;

    private AuthenticatedUser adminUser;
    private SchoolSettingsEntity sampleSettings;

    private static RequestPostProcessor authenticated(AuthenticatedUser user) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null,
                    java.util.List.of(new SimpleGrantedAuthority(user.getRole())))
            );
            return request;
        };
    }

    @BeforeEach
    void setUp() {
        adminUser = new AuthenticatedUser("admin@escola.com", "ROLE_ADMIN", 1L, "ext-admin");

        sampleSettings = SchoolSettingsEntity.builder()
                .schoolId(1L)
                .twoFactorEnabled(false)
                .notifyEnrollment(true)
                .notifyDelinquency(false)
                .notifyExamReminder(true)
                .backupSchedule("DAILY_04")
                .apiKey("uebi_live_abc123def456")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getSettings_shouldReturn200() throws Exception {
        when(settingsRepository.findById(1L)).thenReturn(Optional.of(sampleSettings));

        mockMvc.perform(get("/api/v1/schools/1/settings")
                        .with(authenticated(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.twoFactorEnabled").value(false))
                .andExpect(jsonPath("$.notifyEnrollment").value(true))
                .andExpect(jsonPath("$.backupSchedule").value("DAILY_04"))
                .andExpect(jsonPath("$.apiKey").value("uebi_live_abc123def456"));
    }

    @Test
    void getSettings_shouldCreateDefaultWhenNotFound() throws Exception {
        when(settingsRepository.findById(1L)).thenReturn(Optional.empty());
        SchoolEntity schoolEntity = SchoolEntity.builder().id(1L).name("Escola").build();
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(schoolEntity));
        SchoolSettingsEntity defaultSettings = SchoolSettingsEntity.builder()
                .schoolId(1L).school(schoolEntity)
                .twoFactorEnabled(false).notifyEnrollment(true)
                .notifyDelinquency(false).notifyExamReminder(true)
                .backupSchedule("DAILY_04")
                .build();
        when(settingsRepository.save(any(SchoolSettingsEntity.class))).thenReturn(defaultSettings);

        mockMvc.perform(get("/api/v1/schools/1/settings")
                        .with(authenticated(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.twoFactorEnabled").value(false));
    }

    @Test
    void updateSettings_shouldReturn200() throws Exception {
        when(settingsRepository.findById(1L)).thenReturn(Optional.of(sampleSettings));
        when(settingsRepository.save(any(SchoolSettingsEntity.class))).thenReturn(sampleSettings);
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenReturn(null);

        String json = """
                {
                    "twoFactorEnabled": true,
                    "notifyEnrollment": false,
                    "backupSchedule": "WEEKLY_SUN"
                }
                """;

        mockMvc.perform(put("/api/v1/schools/1/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(adminUser)))
                .andExpect(status().isOk());
    }

    @Test
    void generateApiKey_shouldReturn200() throws Exception {
        when(settingsRepository.findById(1L)).thenReturn(Optional.of(sampleSettings));
        when(settingsRepository.save(any(SchoolSettingsEntity.class))).thenReturn(sampleSettings);
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenReturn(null);

        mockMvc.perform(post("/api/v1/schools/1/settings/generate-api-key")
                        .with(authenticated(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKey").isNotEmpty());
    }

    @Test
    void getAuditLogs_shouldReturn200() throws Exception {
        AuditLogEntity log = AuditLogEntity.builder()
                .id(1L).schoolId(1L).userEmail("admin@escola.com")
                .action("LOGIN").details("Login realizado")
                .createdAt(LocalDateTime.now())
                .build();
        when(auditLogRepository.findBySchoolIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        mockMvc.perform(get("/api/v1/schools/1/settings/audit-logs")
                        .with(authenticated(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].userEmail").value("admin@escola.com"))
                .andExpect(jsonPath("$[0].action").value("LOGIN"));
    }

    @Test
    void getAuditLogs_withPagination_shouldReturn200() throws Exception {
        when(auditLogRepository.findBySchoolIdOrderByCreatedAtDesc(eq(1L), eq(PageRequest.of(1, 10))))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/schools/1/settings/audit-logs")
                        .param("page", "1")
                        .param("size", "10")
                        .with(authenticated(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createAuditLog_shouldReturn201() throws Exception {
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenReturn(null);

        String json = """
                {
                    "action": "EXPORT_DATA",
                    "details": "Dados exportados para CSV"
                }
                """;

        mockMvc.perform(post("/api/v1/schools/1/settings/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(adminUser)))
                .andExpect(status().isCreated());
    }

    // ===================== Forbidden tests =====================

    @Test
    void getSettings_shouldReturn403WhenAdminDifferentSchool() throws Exception {
        AuthenticatedUser otherAdmin = new AuthenticatedUser("admin@outra.com", "ROLE_ADMIN", 2L, "ext-other");

        mockMvc.perform(get("/api/v1/schools/1/settings")
                        .with(authenticated(otherAdmin)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSettings_shouldReturn403WhenAdminDifferentSchool() throws Exception {
        AuthenticatedUser otherAdmin = new AuthenticatedUser("admin@outra.com", "ROLE_ADMIN", 2L, "ext-other");

        String json = """
                {
                    "twoFactorEnabled": true
                }
                """;

        mockMvc.perform(put("/api/v1/schools/1/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(otherAdmin)))
                .andExpect(status().isForbidden());
    }

    @Test
    void generateApiKey_shouldReturn403WhenAdminDifferentSchool() throws Exception {
        AuthenticatedUser otherAdmin = new AuthenticatedUser("admin@outra.com", "ROLE_ADMIN", 2L, "ext-other");

        mockMvc.perform(post("/api/v1/schools/1/settings/generate-api-key")
                        .with(authenticated(otherAdmin)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAuditLogs_shouldReturn403WhenAdminDifferentSchool() throws Exception {
        AuthenticatedUser otherAdmin = new AuthenticatedUser("admin@outra.com", "ROLE_ADMIN", 2L, "ext-other");

        mockMvc.perform(get("/api/v1/schools/1/settings/audit-logs")
                        .with(authenticated(otherAdmin)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAuditLog_shouldReturn403WhenAdminDifferentSchool() throws Exception {
        AuthenticatedUser otherAdmin = new AuthenticatedUser("admin@outra.com", "ROLE_ADMIN", 2L, "ext-other");

        String json = """
                {
                    "action": "HACKER",
                    "details": "Tentativa de acesso"
                }
                """;

        mockMvc.perform(post("/api/v1/schools/1/settings/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(otherAdmin)))
                .andExpect(status().isForbidden());
    }

    // ===================== Validation tests =====================

    @Test
    void updateSettings_shouldReturn400WhenBodyIsEmpty() throws Exception {
        mockMvc.perform(put("/api/v1/schools/1/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("")
                        .with(authenticated(adminUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSettings_shouldReturn400WhenBodyIsInvalidJson() throws Exception {
        mockMvc.perform(put("/api/v1/schools/1/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json")
                        .with(authenticated(adminUser)))
                .andExpect(status().isBadRequest());
    }

    // ===================== CEO access tests =====================

    @Test
    void getSettings_shouldReturn200WhenCeo() throws Exception {
        AuthenticatedUser ceoUser = new AuthenticatedUser("ceo@uebi.com", "ROLE_CEO", null, "ext-ceo");
        when(settingsRepository.findById(1L)).thenReturn(Optional.of(sampleSettings));

        mockMvc.perform(get("/api/v1/schools/1/settings")
                        .with(authenticated(ceoUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backupSchedule").value("DAILY_04"));
    }
}
