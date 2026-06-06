package br.com.uebiescola.core.infrastructure.persistence.repository;

import br.com.uebiescola.core.infrastructure.persistence.entity.ReferralEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaReferralRepository extends JpaRepository<ReferralEntity, Long> {

    List<ReferralEntity> findByReferrerSchoolIdOrderByCreatedAtDesc(Long referrerSchoolId);

    Optional<ReferralEntity> findByReferredSchoolId(Long referredSchoolId);

    long countByReferrerSchoolIdAndStatus(Long referrerSchoolId, String status);
}
