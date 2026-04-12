package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.service.TokenService;
import br.com.uebiescola.core.infrastructure.security.SecurityFilter;
import br.com.uebiescola.core.domain.model.enums.UserRole;
import br.com.uebiescola.core.infrastructure.persistence.entity.AccessLevelEntity;
import br.com.uebiescola.core.infrastructure.persistence.entity.UserEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaAccessLevelRepository;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaUserRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private SecurityFilter securityFilter;

    @MockBean
    private JpaUserRepository userRepository;

    @MockBean
    private JpaAccessLevelRepository accessLevelRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private AuthenticatedUser adminUser;
    private AuthenticatedUser ceoUser;
    private UserEntity sampleUserEntity;

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

        sampleUserEntity = UserEntity.builder()
                .id(1L)
                .externalId(UUID.randomUUID())
                .name("João Silva")
                .cpf("123.456.789-00")
                .email("joao@escola.com")
                .password("encodedPassword")
                .role(UserRole.ROLE_TEACHER)
                .schoolId(1L)
                .active(true)
                .accessLevelId(null)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== School Users ====================

    @Test
    void listSchoolUsers_shouldReturn200() throws Exception {
        when(userRepository.findAllBySchoolId(1L)).thenReturn(List.of(sampleUserEntity));

        mockMvc.perform(get("/api/v1/users")
                        .with(authenticated(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("João Silva"))
                .andExpect(jsonPath("$[0].email").value("joao@escola.com"))
                .andExpect(jsonPath("$[0].role").value("ROLE_TEACHER"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void listSchoolUsers_withSchoolIdParam_shouldReturn200() throws Exception {
        when(userRepository.findAllBySchoolId(2L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users")
                        .param("schoolId", "2")
                        .with(authenticated(ceoUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createSchoolUser_shouldReturn201() throws Exception {
        when(userRepository.existsByEmail("novo@escola.com")).thenReturn(false);
        when(userRepository.existsByCpf("987.654.321-00")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("encodedPassword");

        UserEntity saved = UserEntity.builder()
                .id(2L).externalId(UUID.randomUUID())
                .name("Maria").cpf("987.654.321-00").email("novo@escola.com")
                .password("encodedPassword").role(UserRole.ROLE_TEACHER)
                .schoolId(1L).active(true).build();
        when(userRepository.save(any(UserEntity.class))).thenReturn(saved);

        String json = """
                {
                    "name": "Maria",
                    "cpf": "987.654.321-00",
                    "email": "novo@escola.com",
                    "password": "senha123",
                    "role": "ROLE_TEACHER"
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(adminUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Maria"))
                .andExpect(jsonPath("$.email").value("novo@escola.com"));
    }

    @Test
    void createSchoolUser_shouldReturn409WhenEmailExists() throws Exception {
        when(userRepository.existsByEmail("joao@escola.com")).thenReturn(true);

        String json = """
                {
                    "name": "João",
                    "email": "joao@escola.com",
                    "password": "senha123",
                    "role": "ROLE_TEACHER"
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(adminUser)))
                .andExpect(status().isConflict());
    }

    @Test
    void createSchoolUser_shouldReturn409WhenCpfExists() throws Exception {
        when(userRepository.existsByEmail("novo@escola.com")).thenReturn(false);
        when(userRepository.existsByCpf("123.456.789-00")).thenReturn(true);

        String json = """
                {
                    "name": "Teste",
                    "cpf": "123.456.789-00",
                    "email": "novo@escola.com",
                    "password": "senha123",
                    "role": "ROLE_TEACHER"
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(adminUser)))
                .andExpect(status().isConflict());
    }

    @Test
    void createSchoolUser_shouldReturn400WhenNoPassword() throws Exception {
        when(userRepository.existsByEmail("novo@escola.com")).thenReturn(false);

        String json = """
                {
                    "name": "Teste",
                    "email": "novo@escola.com",
                    "role": "ROLE_TEACHER"
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(adminUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUser_shouldReturn200() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUserEntity));

        mockMvc.perform(get("/api/v1/users/1")
                        .with(authenticated(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("João Silva"));
    }

    @Test
    void getUser_shouldReturn404WhenNotFound() throws Exception {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/users/999")
                        .with(authenticated(adminUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateSchoolUser_shouldReturn200() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUserEntity));
        when(userRepository.save(any(UserEntity.class))).thenReturn(sampleUserEntity);

        String json = """
                {
                    "name": "João Silva Atualizado",
                    "email": "joao.novo@escola.com"
                }
                """;

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(adminUser)))
                .andExpect(status().isOk());
    }

    @Test
    void updateSchoolUser_shouldReturn404WhenNotFound() throws Exception {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        String json = """
                {
                    "name": "Teste"
                }
                """;

        mockMvc.perform(put("/api/v1/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(adminUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void toggleStatus_shouldReturn200() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUserEntity));
        when(userRepository.save(any(UserEntity.class))).thenReturn(sampleUserEntity);

        String json = objectMapper.writeValueAsString(Map.of("active", false));

        mockMvc.perform(patch("/api/v1/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(adminUser)))
                .andExpect(status().isOk());
    }

    @Test
    void toggleStatus_shouldReturn404WhenNotFound() throws Exception {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        String json = objectMapper.writeValueAsString(Map.of("active", false));

        mockMvc.perform(patch("/api/v1/users/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(adminUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_shouldReturn204() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUserEntity));

        mockMvc.perform(delete("/api/v1/users/1")
                        .with(authenticated(adminUser)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_shouldReturn404WhenNotFound() throws Exception {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/users/999")
                        .with(authenticated(adminUser)))
                .andExpect(status().isNotFound());
    }

    // ==================== CEO Team ====================

    @Test
    void listCeoTeam_shouldReturn200() throws Exception {
        UserEntity ceoMember = UserEntity.builder()
                .id(10L).externalId(UUID.randomUUID())
                .name("CEO Member").email("member@uebi.com")
                .password("encoded").role(UserRole.ROLE_CEO)
                .schoolId(null).active(true).build();
        when(userRepository.findAllBySchoolIdIsNull()).thenReturn(List.of(ceoMember));

        mockMvc.perform(get("/api/v1/users/ceo-team")
                        .with(authenticated(ceoUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("CEO Member"))
                .andExpect(jsonPath("$[0].role").value("ROLE_CEO"));
    }

    @Test
    void createCeoTeamMember_shouldReturn201() throws Exception {
        when(userRepository.existsByEmail("newceo@uebi.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("encodedPassword");

        UserEntity saved = UserEntity.builder()
                .id(11L).externalId(UUID.randomUUID())
                .name("Novo CEO").email("newceo@uebi.com")
                .password("encodedPassword").role(UserRole.ROLE_CEO)
                .schoolId(null).active(true).build();
        when(userRepository.save(any(UserEntity.class))).thenReturn(saved);

        String json = """
                {
                    "name": "Novo CEO",
                    "email": "newceo@uebi.com",
                    "password": "senha123"
                }
                """;

        mockMvc.perform(post("/api/v1/users/ceo-team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(ceoUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Novo CEO"))
                .andExpect(jsonPath("$.role").value("ROLE_CEO"));
    }

    @Test
    void createCeoTeamMember_shouldReturn409WhenEmailExists() throws Exception {
        when(userRepository.existsByEmail("existing@uebi.com")).thenReturn(true);

        String json = """
                {
                    "name": "Teste",
                    "email": "existing@uebi.com",
                    "password": "senha123"
                }
                """;

        mockMvc.perform(post("/api/v1/users/ceo-team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(ceoUser)))
                .andExpect(status().isConflict());
    }

    @Test
    void createCeoTeamMember_shouldReturn400WhenNoPassword() throws Exception {
        when(userRepository.existsByEmail("novo@uebi.com")).thenReturn(false);

        String json = """
                {
                    "name": "Teste",
                    "email": "novo@uebi.com"
                }
                """;

        mockMvc.perform(post("/api/v1/users/ceo-team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(ceoUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCeoTeamMember_shouldReturn200() throws Exception {
        UserEntity ceoMember = UserEntity.builder()
                .id(10L).externalId(UUID.randomUUID())
                .name("CEO Member").email("member@uebi.com")
                .password("encoded").role(UserRole.ROLE_CEO)
                .schoolId(null).active(true).build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(ceoMember));
        when(userRepository.save(any(UserEntity.class))).thenReturn(ceoMember);

        String json = """
                {
                    "name": "CEO Member Atualizado"
                }
                """;

        mockMvc.perform(put("/api/v1/users/ceo-team/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(ceoUser)))
                .andExpect(status().isOk());
    }

    @Test
    void updateCeoTeamMember_shouldReturn404WhenNotFound() throws Exception {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        String json = """
                {
                    "name": "Teste"
                }
                """;

        mockMvc.perform(put("/api/v1/users/ceo-team/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(ceoUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCeoTeamMember_shouldReturn404WhenSchoolIdNotNull() throws Exception {
        // Should not be accessible for users with schoolId (not CEO team)
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUserEntity)); // has schoolId=1

        String json = """
                {
                    "name": "Teste"
                }
                """;

        mockMvc.perform(put("/api/v1/users/ceo-team/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(ceoUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void toggleCeoTeamStatus_shouldReturn200() throws Exception {
        UserEntity ceoMember = UserEntity.builder()
                .id(10L).externalId(UUID.randomUUID())
                .name("CEO Member").email("member@uebi.com")
                .password("encoded").role(UserRole.ROLE_CEO)
                .schoolId(null).active(true).build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(ceoMember));
        when(userRepository.save(any(UserEntity.class))).thenReturn(ceoMember);

        String json = objectMapper.writeValueAsString(Map.of("active", false));

        mockMvc.perform(patch("/api/v1/users/ceo-team/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(ceoUser)))
                .andExpect(status().isOk());
    }

    @Test
    void toggleCeoTeamStatus_shouldReturn404WhenNotFound() throws Exception {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        String json = objectMapper.writeValueAsString(Map.of("active", false));

        mockMvc.perform(patch("/api/v1/users/ceo-team/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(authenticated(ceoUser)))
                .andExpect(status().isNotFound());
    }
}
