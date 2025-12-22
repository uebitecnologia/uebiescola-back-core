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
@Filter(name = "tenantFilter", condition = "id = :tenantId") // Para a tabela Schools, filtramos pelo próprio ID
public class SchoolEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private UUID externalId = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String cnpj;

    @Column(unique = true, nullable = false)
    private String subdomain;

    private Boolean active;

    @CreationTimestamp
    private LocalDateTime createdAt;
}