package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.service.TokenService;
import br.com.uebiescola.core.infrastructure.security.SecurityFilter;
import br.com.uebiescola.core.infrastructure.persistence.entity.AuditLogEntity;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaAuditLogRepository;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private SecurityFilter securityFilter;

    @MockBean
    private JpaAuditLogRepository auditLogRepository;

    @MockBean
    private JpaSchoolRepository schoolRepository;

    private AuditLogEntity sampleLog;

    @BeforeEach
    void setUp() {
        sampleLog = AuditLogEntity.builder()
                .id(1L)
                .schoolId(1L)
                .userEmail("admin@escola.com")
                .action("LOGIN")
                .details("Usuário fez login")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllAuditLogs_shouldReturn200() throws Exception {
        when(auditLogRepository.findAllWithFilters(isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(sampleLog)));

        SchoolEntity school = SchoolEntity.builder().id(1L).name("Escola Teste").build();
        when(schoolRepository.findAllById(anyList())).thenReturn(List.of(school));

        mockMvc.perform(get("/api/v1/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].schoolId").value(1))
                .andExpect(jsonPath("$[0].schoolName").value("Escola Teste"))
                .andExpect(jsonPath("$[0].userEmail").value("admin@escola.com"))
                .andExpect(jsonPath("$[0].action").value("LOGIN"));
    }

    @Test
    void getAllAuditLogs_withFilters_shouldReturn200() throws Exception {
        when(auditLogRepository.findAllWithFilters(eq(1L), eq("LOGIN"), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(sampleLog)));

        SchoolEntity school = SchoolEntity.builder().id(1L).name("Escola Teste").build();
        when(schoolRepository.findAllById(anyList())).thenReturn(List.of(school));

        mockMvc.perform(get("/api/v1/audit")
                        .param("schoolId", "1")
                        .param("action", "LOGIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("LOGIN"));
    }

    @Test
    void getAllAuditLogs_emptyResults_shouldReturn200() throws Exception {
        when(auditLogRepository.findAllWithFilters(isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(schoolRepository.findAllById(anyList())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAllAuditLogs_withPagination_shouldReturn200() throws Exception {
        when(auditLogRepository.findAllWithFilters(isNull(), isNull(), isNull(), isNull(), eq(PageRequest.of(1, 10))))
                .thenReturn(new PageImpl<>(List.of(sampleLog)));

        SchoolEntity school = SchoolEntity.builder().id(1L).name("Escola Teste").build();
        when(schoolRepository.findAllById(anyList())).thenReturn(List.of(school));

        mockMvc.perform(get("/api/v1/audit")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void createAuditLog_shouldReturn201() throws Exception {
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenReturn(sampleLog);

        String json = objectMapper.writeValueAsString(Map.of(
                "schoolId", 1,
                "userEmail", "admin@escola.com",
                "action", "LOGIN",
                "details", "Usuário fez login"
        ));

        mockMvc.perform(post("/api/v1/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void createAuditLog_withoutSchoolId_shouldReturn201() throws Exception {
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenReturn(sampleLog);

        String json = objectMapper.writeValueAsString(Map.of(
                "userEmail", "ceo@uebi.com",
                "action", "GLOBAL_SETTINGS_UPDATED",
                "details", "Configurações globais atualizadas"
        ));

        mockMvc.perform(post("/api/v1/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }
}
