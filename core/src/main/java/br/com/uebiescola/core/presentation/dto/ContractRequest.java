package br.com.uebiescola.core.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ContractRequest(String planBase, List<String> activeModules, BigDecimal monthlyValue, BigDecimal setupValue, Integer expirationDay, LocalDate startDate) {}
