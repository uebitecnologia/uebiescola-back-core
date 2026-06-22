package br.com.uebiescola.core.presentation.dto;

import br.com.uebiescola.core.presentation.validation.ValidCNPJ;
import br.com.uebiescola.core.presentation.validation.ValidCPF;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Self-service signup. PJ (escola) preenche cnpj+legalName. PF (Solo)
 * preenche cpf+birthDate. Controller valida que exatamente um caminho
 * esta completo.
 */
public record SchoolRegistrationRequest(
        @NotBlank String schoolName,

        // PJ
        @ValidCNPJ String cnpj,
        String legalName,

        // PF (Solo)
        @ValidCPF String cpf,
        LocalDate birthDate,

        @NotBlank String adminName,
        @NotBlank @Email String adminEmail,
        @NotBlank String adminCpf,
        @NotBlank @Size(min = 6) String adminPassword,
        String phone,
        String subdomain,
        String referralCode,
        // UUID do plano escolhido no site /precos (opcional — se null, trial pega plano mais barato)
        java.util.UUID planUuid,
        // Cartao pre-autorizado (opcional — recomendado pra proteger trial contra abuso)
        CreditCardInput creditCard,
        CreditCardHolderInput creditCardHolderInfo
) {
    /** true se preencheu o caminho PJ (cnpj). */
    public boolean isPj() { return cnpj != null && !cnpj.isBlank(); }
    /** true se preencheu o caminho PF (cpf). */
    public boolean isPf() { return cpf != null && !cpf.isBlank(); }

    public record CreditCardInput(
            String holderName, String number, String expiryMonth, String expiryYear, String ccv
    ) {}

    public record CreditCardHolderInput(
            String name, String email, String cpfCnpj, String postalCode,
            String addressNumber, String phone
    ) {}
}
