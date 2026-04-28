package br.com.uebiescola.core.presentation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CnpjValidator implements ConstraintValidator<ValidCNPJ, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isBlank()) return false;
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != 14) return false;
        if (digits.chars().distinct().count() == 1) return false;

        int[] mult1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] mult2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (digits.charAt(i) - '0') * mult1[i];
        int d1 = sum % 11;
        d1 = d1 < 2 ? 0 : 11 - d1;
        if ((digits.charAt(12) - '0') != d1) return false;

        sum = 0;
        for (int i = 0; i < 13; i++) sum += (digits.charAt(i) - '0') * mult2[i];
        int d2 = sum % 11;
        d2 = d2 < 2 ? 0 : 11 - d2;
        return (digits.charAt(13) - '0') == d2;
    }
}
