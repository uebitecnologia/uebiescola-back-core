package br.com.uebiescola.core.presentation.dto;

import br.com.uebiescola.core.presentation.validation.ValidCNPJ;
import br.com.uebiescola.core.presentation.validation.ValidCPF;

import java.time.LocalDate;

public record SchoolRequest(
        String name,
        String legalName,                // obrigatorio pra PJ
        @ValidCNPJ String cnpj,          // PJ
        @ValidCPF String cpf,            // PF (Solo)
        LocalDate birthDate,             // PF (Solo) — exigido pelo Asaas
        String stateRegistration,
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
) {
    public boolean isPj() { return cnpj != null && !cnpj.isBlank(); }
    public boolean isPf() { return cpf != null && !cpf.isBlank(); }
}
