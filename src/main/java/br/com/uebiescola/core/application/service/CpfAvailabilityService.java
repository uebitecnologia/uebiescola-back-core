package br.com.uebiescola.core.application.service;

import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolRepository;
import br.com.uebiescola.core.presentation.dto.CpfAvailabilityResponse;
import br.com.uebiescola.core.presentation.validation.CpfValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Valida CPF (digitos verificadores) e checa duplicidade contra a tabela schools
 * (plano Solo grava CPF em schools.cpf). Sem chamada externa.
 */
@Service
@RequiredArgsConstructor
public class CpfAvailabilityService {

    private final JpaSchoolRepository schoolRepository;
    private final CpfValidator cpfValidator = new CpfValidator();

    public CpfAvailabilityResponse check(String rawCpf) {
        String digits = rawCpf == null ? "" : rawCpf.replaceAll("\\D", "");

        if (digits.length() != 11 || !cpfValidator.isValid(digits, null)) {
            return CpfAvailabilityResponse.invalid("CPF invalido. Verifique os digitos e tente novamente.");
        }

        boolean alreadyRegistered = schoolRepository.existsByCpf(digits);
        return CpfAvailabilityResponse.ok(digits, alreadyRegistered);
    }
}
