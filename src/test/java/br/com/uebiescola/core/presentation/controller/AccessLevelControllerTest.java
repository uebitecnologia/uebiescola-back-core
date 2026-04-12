package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.service.TokenService;
import br.com.uebiescola.core.infrastructure.security.SecurityFilter;
import br.com.uebiescola.core.infrastructure.persistence.entity.AccessLevelEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaAccessLevelRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccessLevelController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccessLevelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private SecurityFilter securityFilter;

    @MockBean
    private JpaAccessLevelRepository repository;

    private AuthenticatedUser adminUser;
    private AuthenticatedUser ceoUser;
    private AccessLevelEntity sampleEntity;

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
        ceoUser = new AuthenticatedUser("ceo@uebi.com", "ROLE_CEO", null, "ext-ceo");

        sampleEntity = AccessLevelEntity.builder()
                .id(1L)
                .schoolId(1L)
                .name("Professor")
                .description("Nível de acesso para professores")
                .permissions("READ,WRITE")
                .active(true)
                .systemDefault(false)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void list_shouldReturn200() throws Exception {
        when(repository.findAllBySchoolIdOrderByNameAsc(1L)).thenReturn(List.of(sampleEntity));

        mockMvc.perform(get("/api/v1/access-levels")
                        .with(authenticated(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Professor"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void listActive_shouldReturn200() throws Exception {
        when(repository.findAllBySchoolIdAndActiveTrue(1L)).thenReturn(List.of(sampleEntity));

        mockMvc.perform(get("/api/v1/access-levels/active")
                        .with(authenticated(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void getById_shouldReturn200WhenFound() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleEntity));

        mockMvc.perform(get("/api/v1/access-levels/1")
                        .with(authenticated(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Professor"));
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/access-levels/999")
                        .with(authenticated(adminUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldReturn201() throws Exception {
        when(repository.save(any(AccessLevelEntity.class))).thenReturn(sampleEntity);

        String json = """
                {
                    "schoolId": 1,
                    "name": "Professor",
                    "description": "Nível de acesso para professores",
                    "permissions": "READ,WRITE"
                }
                """;

        mockMvc.perform(post("/api/v1/access-levels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(adminUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Professor"));
    }

    @Test
    void update_shouldReturn200() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleEntity));
        when(repository.save(any(AccessLevelEntity.class))).thenReturn(sampleEntity);

        String json = """
                {
                    "name": "Professor Atualizado",
                    "description": "Descrição atualizada",
                    "permissions": "READ,WRITE,DELETE"
                }
                """;

        mockMvc.perform(put("/api/v1/access-levels/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(adminUser)))
                .andExpect(status().isOk());
    }

    @Test
    void update_shouldReturn404WhenNotFound() throws Exception {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        String json = """
                {
                    "name": "Nome"
                }
                """;

        mockMvc.perform(put("/api/v1/access-levels/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(adminUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn403ForSystemDefault() throws Exception {
        AccessLevelEntity systemDefault = AccessLevelEntity.builder()
                .id(2L).schoolId(1L).name("System").systemDefault(true).active(true).build();
        when(repository.findById(2L)).thenReturn(Optional.of(systemDefault));

        String json = """
                {
                    "name": "Novo Nome"
                }
                """;

        mockMvc.perform(put("/api/v1/access-levels/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(adminUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void toggleStatus_shouldReturn200() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleEntity));
        when(repository.save(any(AccessLevelEntity.class))).thenReturn(sampleEntity);

        String json = objectMapper.writeValueAsString(Map.of("active", false));

        mockMvc.perform(patch("/api/v1/access-levels/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(adminUser)))
                .andExpect(status().isOk());
    }

    @Test
    void toggleStatus_shouldReturn404WhenNotFound() throws Exception {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        String json = objectMapper.writeValueAsString(Map.of("active", false));

        mockMvc.perform(patch("/api/v1/access-levels/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(adminUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleEntity));

        mockMvc.perform(delete("/api/v1/access-levels/1")
                        .with(authenticated(adminUser)))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn404WhenNotFound() throws Exception {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/access-levels/999")
                        .with(authenticated(adminUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn403ForSystemDefault() throws Exception {
        AccessLevelEntity systemDefault = AccessLevelEntity.builder()
                .id(2L).schoolId(1L).name("System").systemDefault(true).active(true).build();
        when(repository.findById(2L)).thenReturn(Optional.of(systemDefault));

        mockMvc.perform(delete("/api/v1/access-levels/2")
                        .with(authenticated(adminUser)))
                .andExpect(status().isForbidden());
    }
}
