package br.com.uebiescola.core.application.usecase;

import br.com.uebiescola.core.domain.exception.ResourceNotFoundException;
import br.com.uebiescola.core.domain.model.TermsVersion;
import br.com.uebiescola.core.domain.model.enums.TermsType;
import br.com.uebiescola.core.domain.repository.TermsVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManageTermsVersionUseCase {

    private final TermsVersionRepository termsVersionRepository;

    public TermsVersion create(TermsVersion termsVersion) {
        termsVersion.setActive(false);
        return termsVersionRepository.save(termsVersion);
    }

    public TermsVersion update(Long id, TermsVersion updated) {
        TermsVersion existing = termsVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Versão de termos não encontrada: " + id));

        existing.setType(updated.getType());
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        existing.setVersion(updated.getVersion());

        return termsVersionRepository.save(existing);
    }

    @Transactional
    public TermsVersion activate(Long id) {
        TermsVersion termsVersion = termsVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Versão de termos não encontrada: " + id));

        // Desativa todas as versões do mesmo tipo
        termsVersionRepository.deactivateAllByType(termsVersion.getType());

        // Ativa a versão selecionada
        termsVersion.setActive(true);
        return termsVersionRepository.save(termsVersion);
    }

    public List<TermsVersion> listAll() {
        return termsVersionRepository.findAll();
    }

    public Optional<TermsVersion> getById(Long id) {
        return termsVersionRepository.findById(id);
    }

    public Optional<TermsVersion> getLatestActive(TermsType type) {
        return termsVersionRepository.findFirstByTypeAndActiveTrue(type);
    }

    private Long resolveId(UUID uuid) {
        return termsVersionRepository.findByUuid(uuid)
                .map(TermsVersion::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Versão de termos não encontrada: " + uuid));
    }

    public TermsVersion updateByUuid(UUID uuid, TermsVersion updated) {
        return update(resolveId(uuid), updated);
    }

    @Transactional
    public TermsVersion activateByUuid(UUID uuid) {
        return activate(resolveId(uuid));
    }
}
