package br.com.uebiescola.core.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(
        name = "finance-onboarding",
        url = "${feign.finance-service.url:http://localhost:8083/api/v1}/finance/internal"
)
public interface FinanceOnboardingClient {

    @GetMapping("/bank-account/{schoolId}/exists")
    Map<String, Boolean> hasBankAccount(
            @PathVariable Long schoolId,
            @RequestHeader("X-Internal-Token") String internalToken);
}
