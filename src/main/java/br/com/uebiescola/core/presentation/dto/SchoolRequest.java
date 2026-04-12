package br.com.uebiescola.core.presentation.dto;

public record SchoolRequest(
        String name, String legalName, String cnpj, String stateRegistration,
        String primaryColor,
        String pixKey,
        Double lateFeePercentage,
        Double interestRate,
        AddressRequest address,
        ContractRequest contract,
        TechnicalRequest technical
) {}
