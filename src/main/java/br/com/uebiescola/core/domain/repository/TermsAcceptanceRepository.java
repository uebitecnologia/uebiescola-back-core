package br.com.uebiescola.core.domain.repository;

import br.com.uebiescola.core.domain.model.TermsAcceptance;

import java.util.List;

public interface TermsAcceptanceRepository {
    TermsAcceptance save(TermsAcceptance acceptance);
    List<TermsAcceptance> findByUserId(Long userId);
    boolean existsByUserIdAndTermsVersionId(Long userId, Long termsVersionId);
}
