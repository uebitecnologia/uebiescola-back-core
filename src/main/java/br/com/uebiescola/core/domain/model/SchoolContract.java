package br.com.uebiescola.core.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SchoolContract {
    @JsonIgnore
    private Long id;
    private String planBase;
    private List<String> activeModules;
    private BigDecimal monthlyValue;
    private BigDecimal setupValue;
    private Integer expirationDay;
    private LocalDate startDate;
    private String billingCycle;   // MONTHLY, YEARLY
    private String billingType;    // UNDEFINED, PIX, BOLETO, CREDIT_CARD
}
