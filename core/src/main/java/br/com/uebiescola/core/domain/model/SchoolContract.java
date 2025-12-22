package br.com.uebiescola.core.domain.model;

public record SchoolContract(String planBase, java.util.List<String> activeModules, java.math.BigDecimal monthlyValue, java.math.BigDecimal setupValue, Integer expirationDay, java.time.LocalDate startDate) {}