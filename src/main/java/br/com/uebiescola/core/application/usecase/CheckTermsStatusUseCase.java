package br.com.uebiescola.core.application.usecase;

import br.com.uebiescola.core.domain.model.TermsVersion;
import br.com.uebiescola.core.domain.model.enums.TermsType;
import br.com.uebiescola.core.domain.model.enums.UserRole;
import br.com.uebiescola.core.domain.repository.TermsAcceptanceRepository;
import br.com.uebiescola.core.domain.repository.TermsVersionRepository;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckTermsStatusUseCase {

    private final TermsVersionRepository termsVersionRepository;
    private final TermsAcceptanceRepository termsAcceptanceRepository;
    private final JpaUserRepository userRepository;

    /**
     * Retorna a lista de termos ativos que o usuário ainda NÃO aceitou.
     *
     * AUDITORIA-PORTAL-RESPONSAVEL 06/09/2026: DATA_PROCESSING (DPA) e
     * contrato B2B entre Escola (controladora) e UebiEscola (operadora).
     * Responsavel (ROLE_GUARDIAN) e titular, nao parte do contrato — pedir
     * o aceite do DPA polui trilha e confunde quem le "medicacao/humor"
     * pensando que consentiu com o tratamento (Chrome flagou como P0).
     * Portanto, GUARDIAN recebe apenas TERMS_OF_USE + PRIVACY_POLICY.
     */
    public List<TermsVersion> getPendingTerms(Long userId) {
        List<TermsVersion> pending = new ArrayList<>();

        boolean isGuardian = userRepository.findById(userId)
                .map(u -> u.getRole() == UserRole.ROLE_GUARDIAN)
                .orElse(false);

        for (TermsType type : TermsType.values()) {
            if (isGuardian && type == TermsType.DATA_PROCESSING) continue;
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
