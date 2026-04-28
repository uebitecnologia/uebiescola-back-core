package br.com.uebiescola.core.presentation.dto;

import br.com.uebiescola.core.presentation.validation.ValidCNPJ;

public record SchoolRequest(
        String name, String legalName, @ValidCNPJ String cnpj, String stateRegistration,
        String municipalRegistration,
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
        Integer installmentCount, // 1-12, aplicavel a CREDIT_CARD + YEARLY
        String contactPhone   // telefone da escola para cadastro no Asaas
) {}
