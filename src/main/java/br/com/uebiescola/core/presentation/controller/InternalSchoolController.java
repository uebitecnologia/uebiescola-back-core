package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.domain.model.enums.UserRole;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolEntity;
import br.com.uebiescola.core.infrastructure.persistence.entity.UserEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolRepository;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoint INTERNO (service-to-service) para lookup de dados da escola
 * sem expor via nginx (/v1/internal/* não roteia externamente).
 *
 * Usado por outros services (notification, finance, etc) que precisam
 * resolver schoolId → admin email/name pra envio de notificações.
 */
@RestController
@RequestMapping("/api/v1/internal/schools")
@RequiredArgsConstructor
public class InternalSchoolController {

    private final JpaSchoolRepository schoolRepository;
    private final JpaUserRepository userRepository;

    /**
     * Retorna info do admin principal da escola (ROLE_ADMIN, primeiro encontrado).
     * Resposta: { schoolId, schoolName, subdomain, adminEmail, adminName, adminCpf }.
     * Se escola não existe, 404. Se não tem admin, retorna campos null.
     */
    @GetMapping("/{schoolId}/admin")
    public ResponseEntity<Map<String, Object>> getAdmin(@PathVariable Long schoolId) {
        SchoolEntity school = schoolRepository.findById(schoolId).orElse(null);
        if (school == null) {
            return ResponseEntity.notFound().build();
        }

        UserEntity admin = userRepository.findFirstBySchoolIdAndRole(schoolId, UserRole.ROLE_ADMIN).orElse(null);

        Map<String, Object> resp = new HashMap<>();
        resp.put("schoolId", schoolId);
        resp.put("schoolName", school.getName());
        resp.put("subdomain", school.getSubdomain());
        resp.put("adminEmail", admin != null ? admin.getEmail() : null);
        resp.put("adminName", admin != null ? admin.getName() : null);
        resp.put("adminCpf", admin != null ? admin.getCpf() : null);
        return ResponseEntity.ok(resp);
    }
}
