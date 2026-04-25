package br.com.uebiescola.core.application.usecase;

import br.com.uebiescola.core.domain.exception.ResourceNotFoundException;
import br.com.uebiescola.core.domain.model.School;
import br.com.uebiescola.core.domain.repository.SchoolRepository;
import br.com.uebiescola.core.infrastructure.messaging.SchoolDeletedEvent;
import br.com.uebiescola.core.infrastructure.messaging.SchoolEventProducer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Soft-delete da escola + publicação do evento SchoolDeleted no RabbitMQ
 * pra que cada microsserviço marque suas próprias linhas como deletadas
 * (cascata cross-service via eventos, sem FK entre DBs).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeleteSchoolUseCase {

    private final SchoolRepository schoolRepository;
    private final SchoolEventProducer producer;

    @Transactional
    public void execute(Long schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Escola não encontrada: " + schoolId));

        schoolRepository.deleteById(schoolId);

        producer.publishSchoolDeleted(new SchoolDeletedEvent(
                school.getId(),
                school.getExternalId(),
                school.getName(),
                school.getSubdomain(),
                Instant.now()
        ));

        log.info("[CORE] Escola {} ({}) marcada como deletada", schoolId, school.getSubdomain());
    }
}
