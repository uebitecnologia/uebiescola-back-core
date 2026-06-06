package br.com.uebiescola.core.presentation.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Snapshot do programa de indicacao pra escola logada:
 * codigo, URL sharable, contagens e lista de indicados.
 */
@Data
@Builder
public class ReferralStatusResponse {

    private String referralCode;
    private String shareUrl;
    private long pendingCount;
    private long creditedCount;
    private BigDecimal totalEarned;
    private List<ReferralItem> referrals;

    @Data
    @Builder
    public static class ReferralItem {
        private UUID uuid;
        private String referredSchoolName;
        private String status;
        private BigDecimal creditValue;
        private LocalDateTime createdAt;
        private LocalDateTime creditedAt;
    }
}
