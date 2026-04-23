package br.com.uebiescola.core.presentation.dto;

public record SchoolRequest(
        String name, String legalName, String cnpj, String stateRegistration,
        String primaryColor,
        String pixKey,
        Double lateFeePercentage,
        Double interestRate,
        AddressRequest address,
        ContractRequest contract,
        TechnicalRequest technical,

        // Opcional: se fornecido, cria subscription PAGA no Asaas automaticamente
        Long planId,
        String billingType,   // PIX, BOLETO, CREDIT_CARD, UNDEFINED
        String billingCycle,  // MONTHLY, YEARLY
        String contactPhone   // telefone da escola para cadastro no Asaas
) {}
