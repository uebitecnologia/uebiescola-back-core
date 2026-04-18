package br.com.uebiescola.core.infrastructure.persistence.adapter;

import br.com.uebiescola.core.domain.model.TermsAcceptance;
import br.com.uebiescola.core.domain.repository.TermsAcceptanceRepository;
import br.com.uebiescola.core.infrastructure.persistence.entity.TermsAcceptanceEntity;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaTermsAcceptanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JpaTermsAcceptanceRepositoryAdapter implements TermsAcceptanceRepository {

    private final JpaTermsAcceptanceRepository jpaRepository;

    @Override
    public TermsAcceptance save(TermsAcceptance acceptance) {
        TermsAcceptanceEntity entity = toEntity(acceptance);
        TermsAcceptanceEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<TermsAcceptance> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public boolean existsByUserIdAndTermsVersionId(Long userId, Long termsVersionId) {
        return jpaRepository.existsByUserIdAndTermsVersionId(userId, termsVersionId);
    }

    // --- Mappers ---

    private TermsAcceptanceEntity toEntity(TermsAcceptance domain) {
        return TermsAcceptanceEntity.builder()
                .id(domain.getId())
                .schoolId(domain.getSchoolId())
                .userId(domain.getUserId())
                .termsVersionId(domain.getTermsVersionId())
                .acceptedAt(domain.getAcceptedAt())
                .ipAddress(domain.getIpAddress())
                .userAgent(domain.getUserAgent())
                .build();
    }

    private TermsAcceptance toDomain(TermsAcceptanceEntity entity) {
        return TermsAcceptance.builder()
                .id(entity.getId())
                .schoolId(entity.getSchoolId())
                .userId(entity.getUserId())
                .termsVersionId(entity.getTermsVersionId())
                .acceptedAt(entity.getAcceptedAt())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .build();
    }
}
