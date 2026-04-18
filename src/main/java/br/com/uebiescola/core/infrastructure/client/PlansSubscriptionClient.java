package br.com.uebiescola.core.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "plans-subscription-service", url = "${feign.plans-service.url:http://localhost:8082/api/v1}/plans/subscriptions")
public interface PlansSubscriptionClient {

    @PostMapping("/trial")
    TrialSubscriptionResponse createTrialSubscription(@RequestBody TrialSubscriptionRequest request);

    record TrialSubscriptionRequest(Long schoolId, Integer trialDays) {}

    record TrialSubscriptionResponse(
            Long id,
            Long schoolId,
            String planName,
            String status,
            String trialEndDate
    ) {}
}
