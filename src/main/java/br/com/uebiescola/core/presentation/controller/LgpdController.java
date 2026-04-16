package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.infrastructure.persistence.entity.LgpdDataRequestEntity;
import br.com.uebiescola.core.infrastructure.persistence.entity.UserEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaLgpdDataRequestRepository;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaUserRepository;
import br.com.uebiescola.core.infrastructure.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/lgpd")
@RequiredArgsConstructor
public class LgpdController {

    private final JpaUserRepository userRepository;
    private final JpaLgpdDataRequestRepository lgpdDataRequestRepository;

    // ===================== CONSENT =====================

    @PostMapping("/consent")
    @Transactional
    public ResponseEntity<Map<String, Object>> recordConsent(
            @AuthenticationPrincipal AuthenticatedUser auth,
            HttpServletRequest request) {

        UserEntity user = userRepository.findByEmail(auth.getEmail())
                .orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setLgpdConsentAt(LocalDateTime.now());
        user.setLgpdConsentIp(getClientIp(request));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Consentimento LGPD registrado com sucesso",
                "consentAt", user.getLgpdConsentAt().toString()
        ));
    }

    // ===================== DATA PORTABILITY (Art. 18, V) =====================

    @GetMapping("/my-data")
    public ResponseEntity<Map<String, Object>> exportMyData(
            @AuthenticationPrincipal AuthenticatedUser auth) {

        UserEntity user = userRepository.findByEmail(auth.getEmail())
                .orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", user.getName());
        data.put("cpf", user.getCpf());
        data.put("email", user.getEmail());
        data.put("role", user.getRole() != null ? user.getRole().name() : null);
        data.put("schoolId", user.getSchoolId());
        data.put("active", user.getActive());
        data.put("consentAt", user.getLgpdConsentAt());
        data.put("consentIp", user.getLgpdConsentIp());

        return ResponseEntity.ok(data);
    }

    // ===================== DATA REQUESTS =====================

    @PostMapping("/data-request")
    @Transactional
    public ResponseEntity<LgpdDataRequestEntity> createDataRequest(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestBody Map<String, String> body) {

        UserEntity user = userRepository.findByEmail(auth.getEmail())
                .orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String requestType = body.get("requestType");
        if (requestType == null || (!requestType.equals("DATA_EXPORT")
                && !requestType.equals("DATA_DELETION")
                && !requestType.equals("DATA_CORRECTION"))) {
            return ResponseEntity.badRequest().build();
        }

        LgpdDataRequestEntity entity = LgpdDataRequestEntity.builder()
                .userId(user.getId())
                .schoolId(user.getSchoolId())
                .requestType(requestType)
                .status("PENDING")
                .requestedAt(LocalDateTime.now())
                .notes(body.get("notes"))
                .build();

        LgpdDataRequestEntity saved = lgpdDataRequestRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/data-requests")
    public ResponseEntity<List<LgpdDataRequestEntity>> listMyDataRequests(
            @AuthenticationPrincipal AuthenticatedUser auth) {

        UserEntity user = userRepository.findByEmail(auth.getEmail())
                .orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<LgpdDataRequestEntity> requests = lgpdDataRequestRepository.findByUserId(user.getId());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/data-requests/admin")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<List<LgpdDataRequestEntity>> listAllDataRequests() {
        List<LgpdDataRequestEntity> requests = lgpdDataRequestRepository.findAll();
        return ResponseEntity.ok(requests);
    }

    @PatchMapping("/data-requests/{id}/process")
    @PreAuthorize("hasRole('CEO')")
    @Transactional
    public ResponseEntity<?> processDataRequest(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthenticatedUser auth) {

        return lgpdDataRequestRepository.findById(id)
                .map(entity -> {
                    String status = body.get("status");
                    if (status == null || (!status.equals("COMPLETED") && !status.equals("REJECTED"))) {
                        return ResponseEntity.badRequest().body((Object) Map.of("error", "Status deve ser COMPLETED ou REJECTED"));
                    }

                    entity.setStatus(status);
                    entity.setProcessedAt(LocalDateTime.now());
                    entity.setProcessedBy(auth.getEmail());
                    if (body.get("notes") != null) {
                        entity.setNotes(body.get("notes"));
                    }

                    LgpdDataRequestEntity saved = lgpdDataRequestRepository.save(entity);
                    return ResponseEntity.ok((Object) saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ===================== HELPERS =====================

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
