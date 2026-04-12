package br.com.uebiescola.core.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SchoolContract {
    private Long id;
    private String planBase;
    private List<String> activeModules;
    private BigDecimal monthlyValue;
    private BigDecimal setupValue;
    private Integer expirationDay;
    private LocalDate startDate;
}