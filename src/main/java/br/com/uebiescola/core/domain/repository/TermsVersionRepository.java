package br.com.uebiescola.core.domain.repository;

import br.com.uebiescola.core.domain.model.TermsVersion;
import br.com.uebiescola.core.domain.model.enums.TermsType;

import java.util.List;
import java.util.Optional;

public interface TermsVersionRepository {
    TermsVersion save(TermsVersion termsVersion);
    Optional<TermsVersion> findById(Long id);
    List<TermsVersion> findAll();
    Optional<TermsVersion> findFirstByTypeAndActiveTrue(TermsType type);
    List<TermsVersion> findByTypeAndActiveTrue(TermsType type);
    void deactivateAllByType(TermsType type);
}
