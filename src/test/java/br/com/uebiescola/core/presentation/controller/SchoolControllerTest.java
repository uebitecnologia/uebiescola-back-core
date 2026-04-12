package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.service.TokenService;
import br.com.uebiescola.core.infrastructure.security.SecurityFilter;
import br.com.uebiescola.core.application.usecase.CreateSchoolUseCase;
import br.com.uebiescola.core.application.usecase.FindSchoolsUseCase;
import br.com.uebiescola.core.domain.model.School;
import br.com.uebiescola.core.domain.model.User;
import br.com.uebiescola.core.domain.model.enums.UserRole;
import br.com.uebiescola.core.domain.repository.SchoolRepository;
import br.com.uebiescola.core.domain.repository.UserRepository;
import br.com.uebiescola.core.infrastructure.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SchoolController.class)
@AutoConfigureMockMvc(addFilters = false)
class SchoolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private SecurityFilter securityFilter;

    @MockBean
    private CreateSchoolUseCase createSchoolUseCase;

    @MockBean
    private FindSchoolsUseCase findSchoolsUseCase;

    @MockBean
    private SchoolRepository schoolRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private AuthenticatedUser ceoUser;
    private AuthenticatedUser adminUser;
    private School sampleSchool;

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
        ceoUser = new AuthenticatedUser("ceo@uebi.com", "ROLE_CEO", null, "ext-ceo");
        adminUser = new AuthenticatedUser("admin@escola.com", "ROLE_ADMIN", 1L, "ext-admin");

        sampleSchool = School.builder()
                .id(1L)
                .externalId(UUID.randomUUID())
                .name("Escola Teste")
                .legalName("Escola Teste LTDA")
                .cnpj("12.345.678/0001-99")
                .subdomain("escolateste")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_shouldReturn201() throws Exception {
        when(createSchoolUseCase.execute(any(School.class), any())).thenReturn(sampleSchool);

        String json = """
                {
                    "name": "Escola Teste",
                    "legalName": "Escola Teste LTDA",
                    "cnpj": "12.345.678/0001-99",
                    "technical": {
                        "subdomain": "escolateste",
                        "adminName": "Admin",
                        "adminEmail": "admin@escola.com",
                        "adminCpf": "123.456.789-00",
                        "adminPassword": "senha123"
                    }
                }
                """;

        mockMvc.perform(post("/api/v1/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(ceoUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Escola Teste"));
    }

    @Test
    void listAll_shouldReturn200() throws Exception {
        when(findSchoolsUseCase.execute()).thenReturn(List.of(sampleSchool));
        when(userRepository.findFirstBySchoolIdAndRole(anyLong(), eq(UserRole.ROLE_ADMIN))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/schools")
                        .with(authenticated(ceoUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Escola Teste"));
    }

    @Test
    void getById_shouldReturn200WhenCeo() throws Exception {
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(sampleSchool));
        when(userRepository.findFirstBySchoolIdAndRole(1L, UserRole.ROLE_ADMIN)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/schools/1")
                        .with(authenticated(ceoUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Escola Teste"));
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        when(schoolRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/schools/999")
                        .with(authenticated(ceoUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void toggleStatus_shouldReturn204() throws Exception {
        doNothing().when(schoolRepository).updateStatus(1L, true);

        String json = objectMapper.writeValueAsString(Map.of("status", "ACTIVE"));

        mockMvc.perform(patch("/api/v1/schools/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNoContent());
    }

    @Test
    void getBySubdomain_shouldReturn200() throws Exception {
        when(schoolRepository.findBySubdomain("escolateste")).thenReturn(Optional.of(sampleSchool));

        mockMvc.perform(get("/api/v1/schools/tenant/escolateste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Escola Teste"))
                .andExpect(jsonPath("$.subdomain").value("escolateste"));
    }

    @Test
    void getBySubdomain_shouldReturn404WhenNotFound() throws Exception {
        when(schoolRepository.findBySubdomain("inexistente")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/schools/tenant/inexistente"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLogo_shouldReturn200WhenLogoExists() throws Exception {
        School schoolWithLogo = School.builder()
                .id(1L).name("Escola").subdomain("escola")
                .logoBytes(new byte[]{1, 2, 3})
                .logoContentType("image/png")
                .build();
        when(schoolRepository.findBySubdomain("escola")).thenReturn(Optional.of(schoolWithLogo));

        mockMvc.perform(get("/api/v1/schools/tenant/escola/logo"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));
    }

    @Test
    void getLogo_shouldReturn404WhenNoLogo() throws Exception {
        School schoolNoLogo = School.builder()
                .id(1L).name("Escola").subdomain("escola")
                .logoBytes(null)
                .build();
        when(schoolRepository.findBySubdomain("escola")).thenReturn(Optional.of(schoolNoLogo));

        mockMvc.perform(get("/api/v1/schools/tenant/escola/logo"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLogo_shouldReturn404WhenSchoolNotFound() throws Exception {
        when(schoolRepository.findBySubdomain("inexistente")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/schools/tenant/inexistente/logo"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn200() throws Exception {
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(sampleSchool));
        when(schoolRepository.saveWithAdminPassword(any(School.class), isNull())).thenReturn(sampleSchool);

        String json = """
                {
                    "name": "Escola Atualizada",
                    "legalName": "Escola Atualizada LTDA",
                    "cnpj": "12.345.678/0001-99"
                }
                """;

        mockMvc.perform(put("/api/v1/schools/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(ceoUser)))
                .andExpect(status().isOk());
    }

    @Test
    void update_shouldReturn404WhenNotFound() throws Exception {
        when(schoolRepository.findById(999L)).thenReturn(Optional.empty());

        String json = """
                {
                    "name": "Escola",
                    "legalName": "Escola LTDA",
                    "cnpj": "12.345.678/0001-99"
                }
                """;

        mockMvc.perform(put("/api/v1/schools/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(ceoUser)))
                .andExpect(status().isNotFound());
    }

    // ===================== uploadLogo tests =====================

    @Test
    void uploadLogo_shouldReturn200WhenCeo() throws Exception {
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(sampleSchool));
        when(schoolRepository.save(any(School.class))).thenReturn(sampleSchool);

        MockMultipartFile file = new MockMultipartFile(
                "file", "logo.png", "image/png", new byte[]{1, 2, 3, 4});

        mockMvc.perform(multipart("/api/v1/schools/1/logo-upload")
                        .file(file)
                        .with(request -> { request.setMethod("PATCH"); return request; })
                        .with(authenticated(ceoUser)))
                .andExpect(status().isOk());

        verify(schoolRepository).save(any(School.class));
    }

    @Test
    void uploadLogo_shouldReturn200WhenAdminOwnSchool() throws Exception {
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(sampleSchool));
        when(schoolRepository.save(any(School.class))).thenReturn(sampleSchool);

        MockMultipartFile file = new MockMultipartFile(
                "file", "logo.jpg", "image/jpeg", new byte[]{10, 20, 30});

        mockMvc.perform(multipart("/api/v1/schools/1/logo-upload")
                        .file(file)
                        .with(request -> { request.setMethod("PATCH"); return request; })
                        .with(authenticated(adminUser)))
                .andExpect(status().isOk());
    }

    @Test
    void uploadLogo_shouldReturn404WhenSchoolNotFound() throws Exception {
        when(schoolRepository.findById(999L)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
                "file", "logo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/schools/999/logo-upload")
                        .file(file)
                        .with(request -> { request.setMethod("PATCH"); return request; })
                        .with(authenticated(ceoUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadLogo_shouldReturn403WhenAdminDifferentSchool() throws Exception {
        AuthenticatedUser otherAdmin = new AuthenticatedUser("admin@outra.com", "ROLE_ADMIN", 2L, "ext-other");

        MockMultipartFile file = new MockMultipartFile(
                "file", "logo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/schools/1/logo-upload")
                        .file(file)
                        .with(request -> { request.setMethod("PATCH"); return request; })
                        .with(authenticated(otherAdmin)))
                .andExpect(status().isForbidden());
    }

    // ===================== getById forbidden test =====================

    @Test
    void getById_shouldReturn403WhenAdminDifferentSchool() throws Exception {
        AuthenticatedUser otherAdmin = new AuthenticatedUser("admin@outra.com", "ROLE_ADMIN", 2L, "ext-other");

        mockMvc.perform(get("/api/v1/schools/1")
                        .with(authenticated(otherAdmin)))
                .andExpect(status().isForbidden());
    }

    // ===================== update forbidden test =====================

    @Test
    void update_shouldReturn403WhenAdminDifferentSchool() throws Exception {
        AuthenticatedUser otherAdmin = new AuthenticatedUser("admin@outra.com", "ROLE_ADMIN", 2L, "ext-other");

        String json = """
                {
                    "name": "Escola Hackeada",
                    "legalName": "Hacker LTDA",
                    "cnpj": "00.000.000/0001-00"
                }
                """;

        mockMvc.perform(put("/api/v1/schools/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(otherAdmin)))
                .andExpect(status().isForbidden());
    }

    // ===================== create validation test =====================

    @Test
    void create_shouldReturn400WhenBodyIsEmpty() throws Exception {
        mockMvc.perform(post("/api/v1/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("")
                        .with(authenticated(ceoUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenBodyIsInvalidJson() throws Exception {
        mockMvc.perform(post("/api/v1/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json")
                        .with(authenticated(ceoUser)))
                .andExpect(status().isBadRequest());
    }
}
