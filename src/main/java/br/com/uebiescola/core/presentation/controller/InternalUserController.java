package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.infrastructure.persistence.entity.UserEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoints INTERNOS (service-to-service) para lookup de dados de usuário.
 * Não são roteados externamente pelo nginx (/v1/internal/*).
 *
 * Usados por finance, notification, communication etc para enriquecer
 * eventos com nome, cargo e assinatura do disparador.
 */
@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final JpaUserRepository userRepository;

    /**
     * Perfil resumido por email — campos editáveis em "Meu Perfil".
     * Resposta: { email, name, jobTitle, signature }. 404 se não existir.
     */
    @GetMapping("/by-email/{email}/profile")
    public ResponseEntity<Map<String, Object>> getProfileByEmail(@PathVariable String email) {
        UserEntity user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("email", user.getEmail());
        resp.put("name", user.getName());
        resp.put("jobTitle", user.getJobTitle());
        resp.put("signature", user.getSignature());
        return ResponseEntity.ok(resp);
    }

    /**
     * Resolve uma lista de emails em Users existentes — usado pelo
     * communication-service no auto-popular de grupos de turma:
     * com a lista de teachers + guardians da turma (academic + finance),
     * busca quais ja tem conta de login e devolve {externalId, name, email, role}
     * pra criar ChatMembers de uma vez.
     *
     * Emails sem User cadastrado simplesmente nao aparecem na resposta.
     */
    @PostMapping("/by-emails")
    public ResponseEntity<List<Map<String, Object>>> getUsersByEmails(@RequestBody Collection<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<Map<String, Object>> resp = new ArrayList<>();
        for (String email : emails) {
            if (email == null || email.isBlank()) continue;
            userRepository.findByEmail(email).ifPresent(u -> {
                Map<String, Object> m = new HashMap<>();
                m.put("externalId", u.getExternalId().toString());
                m.put("name", u.getName());
                m.put("email", u.getEmail());
                m.put("role", u.getRole() != null ? u.getRole().name() : null);
                m.put("schoolId", u.getSchoolId());
                resp.add(m);
            });
        }
        return ResponseEntity.ok(resp);
    }
}
