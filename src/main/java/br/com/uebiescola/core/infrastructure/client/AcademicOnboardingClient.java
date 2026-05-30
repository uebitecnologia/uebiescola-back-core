package br.com.uebiescola.core.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(
        name = "academic-onboarding",
        url = "${feign.academic-service.url:http://localhost:8084/api/v1/academic}/internal"
)
public interface AcademicOnboardingClient {

    @GetMapping("/onboarding-counts/{schoolId}")
    Map<String, Long> getOnboardingCounts(
            @PathVariable Long schoolId,
            @RequestHeader("X-Internal-Token") String internalToken);
}
