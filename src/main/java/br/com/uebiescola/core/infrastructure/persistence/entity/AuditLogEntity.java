package br.com.uebiescola.core.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A-5 AUDITORIAADMINPLATAFORMA 03/09/2026: nullable pra leitura CEO
    // cross-tenant (ex: listar escolas — nao ha um schoolId alvo unico).
    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(nullable = false)
    private String action;

    private String details;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
