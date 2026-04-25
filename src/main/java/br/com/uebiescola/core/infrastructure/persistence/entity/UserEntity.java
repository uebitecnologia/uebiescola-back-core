package br.com.uebiescola.core.infrastructure.persistence.entity;

import br.com.uebiescola.core.domain.model.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.validator.constraints.br.CPF;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "schoolId", type = Long.class))
@Filter(name = "tenantFilter", condition = "school_id = :schoolId")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID externalId;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @CPF(message = "CPF inválido")
    @Column(unique = true, nullable = false, length = 14)
    private String cpf;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "school_id")
    private Long schoolId;

    @Builder.Default
    @Column
    private Boolean active = true;

    @Column(name = "access_level_id")
    private Long accessLevelId;

    @Column(name = "lgpd_consent_at")
    private LocalDateTime lgpdConsentAt;

    @Column(name = "lgpd_consent_ip", length = 45)
    private String lgpdConsentIp;

    // Verificacao de email pos-cadastro self-service. emailVerifiedAt null
    // = nao verificado; login pode ser bloqueado ate validar.
    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "email_verification_code", length = 6)
    private String emailVerificationCode;

    @Column(name = "email_verification_expires_at")
    private LocalDateTime emailVerificationExpiresAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}