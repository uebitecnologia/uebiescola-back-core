package br.com.uebiescola.core.infrastructure.persistence.repository;

import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JpaSchoolRepository extends JpaRepository<SchoolEntity, Long> {
    Optional<SchoolEntity> findBySubdomain(String subdomain);
}