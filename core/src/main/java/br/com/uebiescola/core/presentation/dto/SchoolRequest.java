package br.com.uebiescola.core.presentation.dto;

public record SchoolRequest(
        String name, String legalName, String cnpj, String stateRegistration,
        AddressRequest address,
        ContractRequest contract,
        TechnicalRequest technical
) {}
