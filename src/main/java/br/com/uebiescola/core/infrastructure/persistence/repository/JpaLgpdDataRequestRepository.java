package br.com.uebiescola.core.infrastructure.persistence.repository;

import br.com.uebiescola.core.infrastructure.persistence.entity.LgpdDataRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaLgpdDataRequestRepository extends JpaRepository<LgpdDataRequestEntity, Long> {
    List<LgpdDataRequestEntity> findByUserId(Long userId);
    List<LgpdDataRequestEntity> findBySchoolId(Long schoolId);
    List<LgpdDataRequestEntity> findByStatus(String status);
}
