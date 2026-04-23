package br.com.uebiescola.core.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "school_contracts")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SchoolContractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne

    @JoinColumn(name = "school_id", nullable = false)
    private SchoolEntity school;

    private String planBase;

    @ElementCollection // Cria a tabela school_contract_modules automaticamente
    @CollectionTable(name = "school_contract_modules", joinColumns = @JoinColumn(name = "contract_id"))
    @Column(name = "module_name")
    private List<String> activeModules;

    private BigDecimal monthlyValue;
    private BigDecimal setupValue;
    private Integer expirationDay;
    private LocalDate startDate;

    // Ciclo e forma de cobranca — refletidos na subscription do plans-service / Asaas
    @Column(name = "billing_cycle")
    private String billingCycle;

    @Column(name = "billing_type")
    private String billingType;
}