package br.com.uebiescola.core.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "schools")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    @Builder.Default
    private UUID externalId = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String legalName;

    @Column(unique = true, nullable = false)
    private String cnpj;

    @Column(unique = true)
    private String stateRegistration;

    @Column(unique = true)
    private String subdomain;

    @Column(name = "logo_bytes")
    private byte[] logoBytes;

    @Column(name = "logo_content_type")
    private String logoContentType;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "pix_key")
    private String pixKey;

    @Column(name = "late_fee_percentage")
    private Double lateFeePercentage;

    @Column(name = "interest_rate")
    private Double interestRate;

    private Boolean active;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // --- RELACIONAMENTOS ---

    @OneToOne(mappedBy = "school", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private SchoolAddressEntity address;

    @OneToOne(mappedBy = "school", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private SchoolContractEntity contract;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "admin_user_id", referencedColumnName = "id")
    private UserEntity adminUser;

    // --- MÉTODOS HELPER ---

    public void setAddress(SchoolAddressEntity address) {
        this.address = address;
        if (address != null) {
            address.setSchool(this);
        }
    }

    public void setContract(SchoolContractEntity contract) {
        this.contract = contract;
        if (contract != null) {
            contract.setSchool(this);
        }
    }

    public void setAdminUser(UserEntity adminUser) {
        this.adminUser = adminUser;
        // CORREÇÃO: Verificamos se o ID existe antes de setar.
        // Se for uma criação nova, o ID será null aqui, e precisaremos
        // atualizar o usuário DEPOIS de salvar a escola no Service.
        if (adminUser != null && this.getId() != null) {
            adminUser.setSchoolId(this.getId());
        }
    }
}