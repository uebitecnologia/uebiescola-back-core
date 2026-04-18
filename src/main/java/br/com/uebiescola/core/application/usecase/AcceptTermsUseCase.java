package br.com.uebiescola.core.application.usecase;

import br.com.uebiescola.core.domain.exception.ResourceNotFoundException;
import br.com.uebiescola.core.domain.model.TermsAcceptance;
import br.com.uebiescola.core.domain.repository.TermsAcceptanceRepository;
import br.com.uebiescola.core.domain.repository.TermsVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AcceptTermsUseCase {

    private final TermsAcceptanceRepository termsAcceptanceRepository;
    private final TermsVersionRepository termsVersionRepository;

    public TermsAcceptance execute(Long userId, Long schoolId, Long termsVersionId, String ipAddress, String userAgent) {
        // Valida que a versão existe
        termsVersionRepository.findById(termsVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("Versão de termos não encontrada: " + termsVersionId));

        // Verifica se já aceitou esta versão
        if (termsAcceptanceRepository.existsByUserIdAndTermsVersionId(userId, termsVersionId)) {
            // Retorna o aceite existente silenciosamente
            return termsAcceptanceRepository.findByUserId(userId).stream()
                    .filter(a -> a.getTermsVersionId().equals(termsVersionId))
                    .findFirst()
                    .orElse(null);
        }

        TermsAcceptance acceptance = TermsAcceptance.builder()
                .userId(userId)
                .schoolId(schoolId)
                .termsVersionId(termsVersionId)
                .acceptedAt(LocalDateTime.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        return termsAcceptanceRepository.save(acceptance);
    }
}
