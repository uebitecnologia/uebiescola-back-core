package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.service.TokenService;
import br.com.uebiescola.core.infrastructure.security.SecurityFilter;
import br.com.uebiescola.core.infrastructure.persistence.entity.GlobalSettingEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaGlobalSettingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GlobalSettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private SecurityFilter securityFilter;

    @MockBean
    private JpaGlobalSettingRepository settingRepository;

    private GlobalSettingEntity sampleSetting;

    @BeforeEach
    void setUp() {
        sampleSetting = GlobalSettingEntity.builder()
                .id(1L)
                .key("WHATSAPP_ENDPOINT")
                .value("https://api.whatsapp.com")
                .category("WHATSAPP")
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAll_shouldReturn200() throws Exception {
        when(settingRepository.findAll()).thenReturn(List.of(sampleSetting));

        mockMvc.perform(get("/api/v1/global-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].key").value("WHATSAPP_ENDPOINT"))
                .andExpect(jsonPath("$[0].value").value("https://api.whatsapp.com"))
                .andExpect(jsonPath("$[0].category").value("WHATSAPP"));
    }

    @Test
    void getAll_withCategory_shouldReturn200() throws Exception {
        when(settingRepository.findByCategory("WHATSAPP")).thenReturn(List.of(sampleSetting));

        mockMvc.perform(get("/api/v1/global-settings")
                        .param("category", "WHATSAPP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].category").value("WHATSAPP"));
    }

    @Test
    void getAll_emptyResults_shouldReturn200() throws Exception {
        when(settingRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/global-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void updateSettings_shouldReturn200() throws Exception {
        when(settingRepository.findByKey("WHATSAPP_ENDPOINT")).thenReturn(Optional.of(sampleSetting));
        when(settingRepository.save(any(GlobalSettingEntity.class))).thenReturn(sampleSetting);

        String json = objectMapper.writeValueAsString(Map.of(
                "WHATSAPP_ENDPOINT", "https://new-api.whatsapp.com"
        ));

        mockMvc.perform(put("/api/v1/global-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].key").value("WHATSAPP_ENDPOINT"));
    }

    @Test
    void updateSettings_newKey_shouldReturn200() throws Exception {
        GlobalSettingEntity newSetting = GlobalSettingEntity.builder()
                .id(2L).key("SMTP_HOST").value("smtp.gmail.com").category("SMTP")
                .updatedAt(LocalDateTime.now()).build();
        when(settingRepository.findByKey("SMTP_HOST")).thenReturn(Optional.empty());
        when(settingRepository.save(any(GlobalSettingEntity.class))).thenReturn(newSetting);

        String json = objectMapper.writeValueAsString(Map.of(
                "SMTP_HOST", "smtp.gmail.com"
        ));

        mockMvc.perform(put("/api/v1/global-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void updateSettings_multipleKeys_shouldReturn200() throws Exception {
        GlobalSettingEntity setting1 = GlobalSettingEntity.builder()
                .id(1L).key("SMTP_HOST").value("smtp.gmail.com").category("SMTP")
                .updatedAt(LocalDateTime.now()).build();
        GlobalSettingEntity setting2 = GlobalSettingEntity.builder()
                .id(2L).key("SMTP_PORT").value("587").category("SMTP")
                .updatedAt(LocalDateTime.now()).build();

        when(settingRepository.findByKey("SMTP_HOST")).thenReturn(Optional.empty());
        when(settingRepository.findByKey("SMTP_PORT")).thenReturn(Optional.empty());
        when(settingRepository.save(any(GlobalSettingEntity.class)))
                .thenReturn(setting1)
                .thenReturn(setting2);

        String json = objectMapper.writeValueAsString(Map.of(
                "SMTP_HOST", "smtp.gmail.com",
                "SMTP_PORT", "587"
        ));

        mockMvc.perform(put("/api/v1/global-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ===================== Validation tests =====================

    @Test
    void updateSettings_shouldReturn400WhenBodyIsEmpty() throws Exception {
        mockMvc.perform(put("/api/v1/global-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSettings_shouldReturn400WhenBodyIsInvalidJson() throws Exception {
        mockMvc.perform(put("/api/v1/global-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest());
    }

    // ===================== Category filter with no results =====================

    @Test
    void getAll_withNonExistentCategory_shouldReturnEmptyList() throws Exception {
        when(settingRepository.findByCategory("NONEXISTENT")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/global-settings")
                        .param("category", "NONEXISTENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ===================== Method not allowed =====================

    @Test
    void globalSettings_shouldReturn405WhenDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/global-settings"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ===================== Update existing key test =====================

    @Test
    void updateSettings_existingKey_shouldUpdateValue() throws Exception {
        when(settingRepository.findByKey("WHATSAPP_ENDPOINT")).thenReturn(Optional.of(sampleSetting));
        GlobalSettingEntity updatedSetting = GlobalSettingEntity.builder()
                .id(1L).key("WHATSAPP_ENDPOINT").value("https://updated-api.whatsapp.com")
                .category("WHATSAPP").updatedAt(LocalDateTime.now()).build();
        when(settingRepository.save(any(GlobalSettingEntity.class))).thenReturn(updatedSetting);

        String json = objectMapper.writeValueAsString(Map.of(
                "WHATSAPP_ENDPOINT", "https://updated-api.whatsapp.com"
        ));

        mockMvc.perform(put("/api/v1/global-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("WHATSAPP_ENDPOINT"))
                .andExpect(jsonPath("$[0].value").value("https://updated-api.whatsapp.com"));
    }
}
