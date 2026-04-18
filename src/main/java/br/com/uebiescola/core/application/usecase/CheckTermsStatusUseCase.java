package br.com.uebiescola.core.application.usecase;

import br.com.uebiescola.core.domain.model.TermsVersion;
import br.com.uebiescola.core.domain.model.enums.TermsType;
import br.com.uebiescola.core.domain.repository.TermsAcceptanceRepository;
import br.com.uebiescola.core.domain.repository.TermsVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckTermsStatusUseCase {

    private final TermsVersionRepository termsVersionRepository;
    private final TermsAcceptanceRepository termsAcceptanceRepository;

    /**
     * Retorna a lista de termos ativos que o usuário ainda NÃO aceitou.
     */
    public List<TermsVersion> getPendingTerms(Long userId) {
        List<TermsVersion> pending = new ArrayList<>();

        for (TermsType type : TermsType.values()) {
            termsVersionRepository.findFirstByTypeAndActiveTrue(type).ifPresent(activeVersion -> {
                boolean accepted = termsAcceptanceRepository.existsByUserIdAndTermsVersionId(
                        userId, activeVersion.getId());
                if (!accepted) {
                    pending.add(activeVersion);
                }
            });
        }

        return pending;
    }
}
