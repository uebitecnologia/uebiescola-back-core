package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.service.ReferralService;
import br.com.uebiescola.core.infrastructure.persistence.entity.ReferralEntity;
import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolRepository;
import br.com.uebiescola.core.infrastructure.security.AuthenticatedUser;
import br.com.uebiescola.core.presentation.dto.ReferralStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/referrals")
@RequiredArgsConstructor
@Slf4j
public class ReferralController {

    private final ReferralService referralService;
    private final JpaSchoolRepository schoolRepository;

    @Value("${uebi.public-site-url:https://uebiescola.com.br}")
    private String publicSiteUrl;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReferralStatusResponse> myStatus(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null || user.getSchoolId() == null) {
            return ResponseEntity.badRequest().build();
        }
        Long schoolId = user.getSchoolId();
        String code = referralService.ensureCodeFor(schoolId);

        List<ReferralEntity> raw = referralService.listForReferrer(schoolId);
        Map<Long, String> nameById = new HashMap<>();
        if (!raw.isEmpty()) {
            List<Long> ids = raw.stream().map(ReferralEntity::getReferredSchoolId).distinct().toList();
            schoolRepository.findAllById(ids).forEach(s -> nameById.put(s.getId(), s.getName()));
        }

        List<ReferralStatusResponse.ReferralItem> items = raw.stream()
                .map(r -> ReferralStatusResponse.ReferralItem.builder()
                        .uuid(r.getUuid())
                        .referredSchoolName(nameById.getOrDefault(r.getReferredSchoolId(), "Escola"))
                        .status(r.getStatus())
                        .creditValue(r.getCreditValue())
                        .createdAt(r.getCreatedAt())
                        .creditedAt(r.getCreditedAt())
                        .build())
                .collect(Collectors.toList());

        long credited = referralService.countCredited(schoolId);
        long pending = referralService.countPending(schoolId);
        BigDecimal totalEarned = items.stream()
                .filter(i -> "CREDITED".equals(i.getStatus()))
                .map(ReferralStatusResponse.ReferralItem::getCreditValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(ReferralStatusResponse.builder()
                .referralCode(code)
                .shareUrl(publicSiteUrl + "/cadastro?ref=" + code)
                .creditedCount(credited)
                .pendingCount(pending)
                .totalEarned(totalEarned)
                .referrals(items)
                .build());
    }
}
