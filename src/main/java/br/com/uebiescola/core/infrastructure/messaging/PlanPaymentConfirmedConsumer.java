package br.com.uebiescola.core.infrastructure.messaging;

import br.com.uebiescola.core.application.service.ReferralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consome `plan.payment.confirmed` (publicado pelo plans-service) e promove
 * a indicacao da escola pagante de PENDING para CREDITED no programa #66.
 *
 * Idempotente: o ReferralService faz guard de status (CREDITED/CANCELLED ja
 * processados nao mudam). Mesma mensagem pode ser entregue 2x sem efeito.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PlanPaymentConfirmedConsumer {

    private final ReferralService referralService;

    @RabbitListener(queues = RabbitMQConfig.PLAN_PAYMENT_CONFIRMED_QUEUE)
    public void onPlanPaymentConfirmed(Map<String, Object> payload) {
        Long schoolId = extractSchoolId(payload);
        if (schoolId == null) {
            log.warn("[CORE] plan.payment.confirmed recebido sem schoolId: {}", payload);
            return;
        }
        try {
            referralService.creditReferralIfPending(schoolId);
        } catch (Exception e) {
            log.error("[CORE] Falha ao creditar indicacao da escola {}: {}", schoolId, e.getMessage(), e);
            throw e; // DLQ
        }
    }

    private Long extractSchoolId(Map<String, Object> payload) {
        Object id = payload.get("schoolId");
        if (id instanceof Number n) return n.longValue();
        if (id instanceof String s) return Long.parseLong(s);
        return null;
    }
}
