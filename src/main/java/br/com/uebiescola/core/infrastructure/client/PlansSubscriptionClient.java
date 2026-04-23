package br.com.uebiescola.core.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "plans-subscription-service", url = "${feign.plans-service.url:http://localhost:8082/api/v1}/plans")
public interface PlansSubscriptionClient {

    /**
     * Cria subscription TRIAL (sem cobranca) para escola recem-criada.
     * Nao usa Asaas. Usado no self-service registration.
     */
    @PostMapping("/subscriptions/trial")
    TrialSubscriptionResponse createTrialSubscription(@RequestBody TrialSubscriptionRequest request);

    record TrialSubscriptionRequest(Long schoolId, Integer trialDays) {}

    record TrialSubscriptionResponse(
            Long id,
            Long schoolId,
            String planName,
            String status,
            String trialEndDate
    ) {}

    /**
     * Cria subscription PAGA (cria customer + subscription no Asaas).
     * Usado quando admin CEO cadastra escola com plano escolhido.
     */
    @PostMapping("/internal/subscriptions/paid")
    Object createPaidSubscription(@RequestBody PaidSubscriptionRequest request);

    record PaidSubscriptionRequest(
            Long schoolId,
            Long planId,
            String billingType,
            String billingCycle,
            String schoolName,
            String cnpj,
            String email,
            String phone,
            String mobilePhone,
            String postalCode,
            String address,
            String addressNumber,
            String complement,
            String province,
            String city,
            String state
    ) {}

    /**
     * Sincroniza a subscription com mudancas feitas no contrato (PUT /schools/{id}).
     * Campos null = manter o valor atual. Se a escola nao tem subscription,
     * nada acontece. Se tem e algo relevante mudou, cancela no Asaas + recria.
     */
    @PostMapping("/internal/subscriptions/sync")
    Object syncSubscription(@RequestBody SyncSubscriptionRequest request);

    record SyncSubscriptionRequest(
            Long schoolId,
            Long planId,
            String billingCycle,
            String billingType,
            String schoolName,
            String cnpj,
            String email,
            String phone
    ) {}

    /**
     * Garante que a escola tem um customer correspondente no Asaas.
     * Chamado em todo CREATE e UPDATE de escola, independente de ter plano pago.
     */
    @PostMapping("/internal/subscriptions/ensure-customer")
    Object ensureAsaasCustomer(@RequestBody EnsureCustomerRequest request);

    record EnsureCustomerRequest(
            Long schoolId,
            String schoolName,
            String cnpj,
            String email,
            String phone,
            String mobilePhone,
            String postalCode,
            String address,
            String addressNumber,
            String complement,
            String province,
            String city,
            String state
    ) {}
}
