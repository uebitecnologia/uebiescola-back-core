package br.com.uebiescola.core.infrastructure.persistence.repository;

import br.com.uebiescola.core.infrastructure.persistence.entity.SchoolSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSchoolSettingsRepository extends JpaRepository<SchoolSettingsEntity, Long> {
}
