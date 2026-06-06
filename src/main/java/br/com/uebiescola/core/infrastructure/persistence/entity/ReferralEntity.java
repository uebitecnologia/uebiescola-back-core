package br.com.uebiescola.core.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Indicacao paga (programa "Indique e ganhe R$200").
 * Status: PENDING (acabou de ser criada), CREDITED (R$200 ja creditados
 * apos primeiro pagamento da escola indicada), CANCELLED (indicada
 * cancelou antes do primeiro pagamento).
 */
@Entity
@Table(name = "referrals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(name = "referrer_school_id", nullable = false)
    private Long referrerSchoolId;

    @Column(name = "referred_school_id", nullable = false, unique = true)
    private Long referredSchoolId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "credit_value", nullable = false)
    private BigDecimal creditValue;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "credited_at")
    private LocalDateTime creditedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @PrePersist
    protected void onCreate() {
        if (this.uuid == null) this.uuid = UUID.randomUUID();
        if (this.status == null) this.status = "PENDING";
        if (this.creditValue == null) this.creditValue = new BigDecimal("200.00");
    }
}
