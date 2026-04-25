package br.com.uebiescola.core.application.usecase;

import br.com.uebiescola.core.domain.exception.ResourceNotFoundException;
import br.com.uebiescola.core.domain.model.School;
import br.com.uebiescola.core.domain.repository.SchoolRepository;
import br.com.uebiescola.core.infrastructure.messaging.SchoolDeletedEvent;
import br.com.uebiescola.core.infrastructure.messaging.SchoolEventProducer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Soft-delete da escola + cascata local (users, school_contracts,
 * terms_acceptances) + publicação do evento SchoolDeleted no RabbitMQ
 * pra que cada microsserviço externo marque suas próprias linhas
 * (cascata cross-service via eventos, sem FK entre DBs).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeleteSchoolUseCase {

    private final SchoolRepository schoolRepository;
    private final SchoolEventProducer producer;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void execute(Long schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Escola não encontrada: " + schoolId));

        // 1) Cascata local (mesmo banco do core)
        int users      = softDelete("users", schoolId);
        int contracts  = softDelete("school_contracts", schoolId);
        int terms      = softDelete("terms_acceptances", schoolId);

        // 2) Soft-delete da própria escola via @SQLDelete
        schoolRepository.deleteById(schoolId);

        // 3) Evento Rabbit pros outros serviços cascatearem
        producer.publishSchoolDeleted(new SchoolDeletedEvent(
                school.getId(),
                school.getExternalId(),
                school.getName(),
                school.getSubdomain(),
                Instant.now()
        ));

        log.info("[CORE] Escola {} ({}) marcada como deletada | users={} contracts={} terms={}",
                schoolId, school.getSubdomain(), users, contracts, terms);
    }

    private int softDelete(String table, Long schoolId) {
        return em.createNativeQuery(
                        "UPDATE " + table + " SET deleted_at = NOW() " +
                        "WHERE school_id = :sid AND deleted_at IS NULL")
                .setParameter("sid", schoolId)
                .executeUpdate();
    }
}
