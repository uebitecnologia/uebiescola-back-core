package br.com.uebiescola.core.infrastructure.persistence.adapter;

import br.com.uebiescola.core.domain.model.TermsVersion;
import br.com.uebiescola.core.domain.model.enums.TermsType;
import br.com.uebiescola.core.domain.repository.TermsVersionRepository;
import br.com.uebiescola.core.infrastructure.persistence.entity.TermsVersionEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaTermsVersionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaTermsVersionRepositoryAdapter implements TermsVersionRepository {

    private final JpaTermsVersionRepository jpaRepository;

    @Override
    public TermsVersion save(TermsVersion termsVersion) {
        TermsVersionEntity entity = toEntity(termsVersion);
        TermsVersionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<TermsVersion> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<TermsVersion> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<TermsVersion> findFirstByTypeAndActiveTrue(TermsType type) {
        return jpaRepository.findFirstByTypeAndActiveTrueOrderByCreatedAtDesc(type)
                .map(this::toDomain);
    }

    @Override
    public List<TermsVersion> findByTypeAndActiveTrue(TermsType type) {
        return jpaRepository.findByTypeAndActiveTrue(type).stream()
                .map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void deactivateAllByType(TermsType type) {
        jpaRepository.deactivateAllByType(type);
    }

    // --- Mappers ---

    private TermsVersionEntity toEntity(TermsVersion domain) {
        return TermsVersionEntity.builder()
                .id(domain.getId())
                .type(domain.getType())
                .title(domain.getTitle())
                .content(domain.getContent())
                .version(domain.getVersion())
                .active(domain.getActive())
                .createdAt(domain.getCreatedAt())
                .createdBy(domain.getCreatedBy())
                .build();
    }

    private TermsVersion toDomain(TermsVersionEntity entity) {
        return TermsVersion.builder()
                .id(entity.getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .content(entity.getContent())
                .version(entity.getVersion())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }
}
