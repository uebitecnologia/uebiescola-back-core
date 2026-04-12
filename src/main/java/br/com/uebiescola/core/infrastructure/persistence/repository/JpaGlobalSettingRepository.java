package br.com.uebiescola.core.infrastructure.persistence.repository;

import br.com.uebiescola.core.infrastructure.persistence.entity.GlobalSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaGlobalSettingRepository extends JpaRepository<GlobalSettingEntity, Long> {

    Optional<GlobalSettingEntity> findByKey(String key);

    List<GlobalSettingEntity> findByCategory(String category);
}
