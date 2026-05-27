package br.com.uebiescola.core.presentation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida CPF (11 digitos + DV). Aceita null/blank — combinacao PF/PJ
 * (pelo menos um dos dois) e validada em outro nivel.
 */
public class CpfValidator implements ConstraintValidator<ValidCPF, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isBlank()) return true; // null OK — combo PF/PJ valida em outro nivel
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != 11) return false;
        if (digits.chars().distinct().count() == 1) return false;

        int sum = 0;
        for (int i = 0; i < 9; i++) sum += (digits.charAt(i) - '0') * (10 - i);
        int d1 = sum % 11;
        d1 = d1 < 2 ? 0 : 11 - d1;
        if ((digits.charAt(9) - '0') != d1) return false;

        sum = 0;
        for (int i = 0; i < 10; i++) sum += (digits.charAt(i) - '0') * (11 - i);
        int d2 = sum % 11;
        d2 = d2 < 2 ? 0 : 11 - d2;
        return (digits.charAt(10) - '0') == d2;
    }
}
