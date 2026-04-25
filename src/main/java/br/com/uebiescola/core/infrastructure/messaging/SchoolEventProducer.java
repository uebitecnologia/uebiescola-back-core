package br.com.uebiescola.core.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SchoolEventProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publica SchoolDeleted como Map pra que cada consumer leia campos sem
     * depender de classe Java compartilhada (cada serviço deserializa em sua
     * própria classe local). Falha publicação não propaga: soft-delete já
     * aconteceu no core e a cascata cross-service eventualmente reconcilia.
     */
    public void publishSchoolDeleted(SchoolDeletedEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schoolId", event.schoolId());
        payload.put("externalId", event.externalId() != null ? event.externalId().toString() : null);
        payload.put("schoolName", event.schoolName());
        payload.put("subdomain", event.subdomain());
        payload.put("deletedAt", event.deletedAt() != null ? event.deletedAt().toString() : null);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SCHOOL_DELETED_EXCHANGE,
                    RabbitMQConfig.SCHOOL_DELETED_KEY,
                    payload
            );
            log.info("[CORE] SchoolDeleted publicado: schoolId={} subdomain={}",
                    event.schoolId(), event.subdomain());
        } catch (Exception e) {
            log.error("[CORE] Falha ao publicar SchoolDeleted (schoolId={}): {}",
                    event.schoolId(), e.getMessage());
        }
    }
}
