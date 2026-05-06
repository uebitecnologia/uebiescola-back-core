package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.domain.model.User;
import br.com.uebiescola.core.domain.model.enums.UserRole;
import br.com.uebiescola.core.infrastructure.persistence.entity.AccessLevelEntity;
import br.com.uebiescola.core.infrastructure.persistence.entity.UserEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaAccessLevelRepository;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaUserRepository;
import br.com.uebiescola.core.infrastructure.security.AuthenticatedUser;
import br.com.uebiescola.core.presentation.dto.UserDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final JpaUserRepository userRepository;
    private final JpaAccessLevelRepository accessLevelRepository;
    private final PasswordEncoder passwordEncoder;

    // ===================== SCHOOL USERS (ADMIN) =====================

    @GetMapping
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    public ResponseEntity<List<UserDTO>> listSchoolUsers(
            @RequestParam(value = "schoolId", required = false) Long requestSchoolId,
            @AuthenticationPrincipal AuthenticatedUser user) {

        Long targetSchoolId;
        if (user.getRole().contains("CEO")) {
            // CEO can pass schoolId as param, or it comes from X-School-Id header
            targetSchoolId = requestSchoolId != null ? requestSchoolId : user.getSchoolId();
            if (targetSchoolId == null) return ResponseEntity.badRequest().build();
        } else {
            targetSchoolId = user.getSchoolId();
        }

        List<UserDTO> users = userRepository.findAllBySchoolId(targetSchoolId).stream()
                .map(this::toDTO)
                .toList();

        return ResponseEntity.ok(users);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    @Transactional
    public ResponseEntity<?> createSchoolUser(
            @RequestBody UserDTO dto,
            @AuthenticationPrincipal AuthenticatedUser user) {

        Long schoolId;
        if (user.getRole().contains("CEO")) {
            // CEO can pass schoolId in body, or it comes from X-School-Id header
            schoolId = dto.schoolId() != null ? dto.schoolId() : user.getSchoolId();
            if (schoolId == null) return ResponseEntity.badRequest().body(Map.of("error", "schoolId é obrigatório"));
        } else {
            schoolId = user.getSchoolId();
        }

        if (userRepository.existsByEmail(dto.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "E-mail já cadastrado"));
        }
        if (dto.cpf() != null && userRepository.existsByCpf(dto.cpf())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "CPF já cadastrado"));
        }
        if (dto.password() == null || dto.password().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Senha é obrigatória"));
        }

        UserRole role;
        try {
            role = UserRole.valueOf(dto.role());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role inválida: " + dto.role()));
        }

        // ADMIN da escola não pode criar CEO
        if (!user.getRole().contains("CEO") && role == UserRole.ROLE_CEO) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Sem permissão para criar usuário CEO"));
        }

        UserEntity entity = UserEntity.builder()
                .externalId(UUID.randomUUID())
                .name(dto.name())
                .cpf(dto.cpf())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .role(role)
                .schoolId(schoolId)
                .active(true)
                .accessLevelId(dto.accessLevelId())
                .build();

        UserEntity saved = userRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    public ResponseEntity<UserDTO> getUser(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return userRepository.findByExternalId(uuid)
                .filter(entity -> hasAccessToUser(user, entity))
                .map(entity -> ResponseEntity.ok(toDTO(entity)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    @Transactional
    public ResponseEntity<?> updateSchoolUser(
            @PathVariable UUID uuid,
            @RequestBody UserDTO dto,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return userRepository.findByExternalId(uuid)
                .filter(entity -> hasAccessToUser(user, entity))
                .map(entity -> {
                    if (dto.name() != null) entity.setName(dto.name());
                    if (dto.cpf() != null) entity.setCpf(dto.cpf());
                    if (dto.email() != null) entity.setEmail(dto.email());
                    if (dto.role() != null) {
                        try {
                            UserRole newRole = UserRole.valueOf(dto.role());
                            if (!user.getRole().contains("CEO") && newRole == UserRole.ROLE_CEO) {
                                return ResponseEntity.status(HttpStatus.FORBIDDEN).body((Object) Map.of("error", "Sem permissão"));
                            }
                            entity.setRole(newRole);
                        } catch (Exception ignored) {}
                    }
                    if (dto.password() != null && !dto.password().isBlank()) {
                        entity.setPassword(passwordEncoder.encode(dto.password()));
                    }
                    if (dto.accessLevelId() != null) {
                        entity.setAccessLevelId(dto.accessLevelId());
                    }

                    UserEntity saved = userRepository.save(entity);
                    return ResponseEntity.ok((Object) toDTO(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{uuid}/status")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    @Transactional
    public ResponseEntity<UserDTO> toggleStatus(
            @PathVariable UUID uuid,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return userRepository.findByExternalId(uuid)
                .filter(entity -> hasAccessToUser(user, entity))
                .map(entity -> {
                    entity.setActive(body.getOrDefault("active", !entity.getActive()));
                    UserEntity saved = userRepository.save(entity);
                    return ResponseEntity.ok(toDTO(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    @Transactional
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return userRepository.findByExternalId(uuid)
                .filter(entity -> hasAccessToUser(user, entity))
                .map(entity -> {
                    userRepository.delete(entity);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ===================== CEO TEAM =====================

    @GetMapping("/ceo-team")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<List<UserDTO>> listCeoTeam() {
        List<UserDTO> users = userRepository.findAllBySchoolIdIsNull().stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/ceo-team")
    @PreAuthorize("hasRole('CEO')")
    @Transactional
    public ResponseEntity<?> createCeoTeamMember(@RequestBody UserDTO dto) {

        if (userRepository.existsByEmail(dto.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "E-mail já cadastrado"));
        }
        if (dto.cpf() != null && !dto.cpf().isBlank() && userRepository.existsByCpf(dto.cpf())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "CPF já cadastrado"));
        }
        if (dto.password() == null || dto.password().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Senha é obrigatória"));
        }

        UserEntity entity = UserEntity.builder()
                .externalId(UUID.randomUUID())
                .name(dto.name())
                .cpf(dto.cpf())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .role(UserRole.ROLE_CEO)
                .schoolId(null)
                .active(true)
                .build();

        UserEntity saved = userRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
    }

    @PutMapping("/ceo-team/{uuid}")
    @PreAuthorize("hasRole('CEO')")
    @Transactional
    public ResponseEntity<?> updateCeoTeamMember(
            @PathVariable UUID uuid,
            @RequestBody UserDTO dto) {

        return userRepository.findByExternalId(uuid)
                .filter(entity -> entity.getSchoolId() == null)
                .map(entity -> {
                    if (dto.name() != null) entity.setName(dto.name());
                    if (dto.cpf() != null) entity.setCpf(dto.cpf());
                    if (dto.email() != null) entity.setEmail(dto.email());
                    if (dto.password() != null && !dto.password().isBlank()) {
                        entity.setPassword(passwordEncoder.encode(dto.password()));
                    }
                    UserEntity saved = userRepository.save(entity);
                    return ResponseEntity.ok((Object) toDTO(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/ceo-team/{uuid}/status")
    @PreAuthorize("hasRole('CEO')")
    @Transactional
    public ResponseEntity<UserDTO> toggleCeoTeamStatus(
            @PathVariable UUID uuid,
            @RequestBody Map<String, Boolean> body) {

        return userRepository.findByExternalId(uuid)
                .filter(entity -> entity.getSchoolId() == null)
                .map(entity -> {
                    entity.setActive(body.getOrDefault("active", !entity.getActive()));
                    UserEntity saved = userRepository.save(entity);
                    return ResponseEntity.ok(toDTO(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ===================== HELPERS =====================

    private boolean hasAccessToUser(AuthenticatedUser auth, UserEntity entity) {
        if (auth.getRole().contains("CEO")) return true;
        return entity.getSchoolId() != null && entity.getSchoolId().equals(auth.getSchoolId());
    }

    private UserDTO toDTO(UserEntity entity) {
        String accessLevelName = null;
        if (entity.getAccessLevelId() != null) {
            accessLevelName = accessLevelRepository.findById(entity.getAccessLevelId())
                    .map(AccessLevelEntity::getName)
                    .orElse(null);
        }
        return new UserDTO(
                entity.getExternalId() != null ? entity.getExternalId().toString() : null,
                entity.getName(),
                entity.getCpf(),
                entity.getEmail(),
                null, // never return password
                entity.getRole() != null ? entity.getRole().name() : null,
                entity.getSchoolId(),
                entity.getActive() != null ? entity.getActive() : true,
                entity.getAccessLevelId(),
                accessLevelName
        );
    }
}
