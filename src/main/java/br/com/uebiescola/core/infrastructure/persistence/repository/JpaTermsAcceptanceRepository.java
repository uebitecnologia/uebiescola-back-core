package br.com.uebiescola.core.infrastructure.persistence.repository;

import br.com.uebiescola.core.infrastructure.persistence.entity.TermsAcceptanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaTermsAcceptanceRepository extends JpaRepository<TermsAcceptanceEntity, Long> {

    List<TermsAcceptanceEntity> findByUserId(Long userId);

    boolean existsByUserIdAndTermsVersionId(Long userId, Long termsVersionId);
}
