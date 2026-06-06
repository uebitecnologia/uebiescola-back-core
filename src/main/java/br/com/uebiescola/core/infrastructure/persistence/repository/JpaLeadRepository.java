package br.com.uebiescola.core.infrastructure.persistence.repository;

import br.com.uebiescola.core.infrastructure.persistence.entity.LeadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaLeadRepository extends JpaRepository<LeadEntity, Long> {

    long countByEmailAndResource(String email, String resource);

    List<LeadEntity> findByResourceOrderByCreatedAtDesc(String resource);
}
