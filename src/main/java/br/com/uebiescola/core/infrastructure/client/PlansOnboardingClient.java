package br.com.uebiescola.core.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(
        name = "plans-onboarding",
        url = "${feign.plans-service.url:http://localhost:8082/api/v1}/plans/internal/asaas"
)
public interface PlansOnboardingClient {

    @GetMapping("/subaccount/{schoolId}/has-payment-configured")
    Map<String, Boolean> hasPaymentConfigured(
            @PathVariable Long schoolId,
            @RequestHeader("X-Internal-Token") String internalToken);
}
