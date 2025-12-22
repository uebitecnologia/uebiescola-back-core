package br.com.uebiescola.core.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "schools")
@Getter
@Setter
@NoArgsConstructor    // Obrigatório para o JPA
@AllArgsConstructor   // Obrigatório para o Builder funcionar
@Builder
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
//@Filter(name = "tenantFilter", condition = "id = :tenantId")
public class SchoolEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private UUID externalId = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String legalName;

    @Column(unique = true, nullable = false)
    private String cnpj;

    @Column(unique = true, nullable = true)
    private String stateRegistration;

    private String subdomain;

    private Boolean active;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "school", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SchoolAddressEntity address;

    @OneToOne(mappedBy = "school", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SchoolContractEntity contract;
}