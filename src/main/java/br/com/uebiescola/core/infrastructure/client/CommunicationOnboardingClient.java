package br.com.uebiescola.core.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(
        name = "communication-onboarding",
        url = "${feign.communication-service.url:http://localhost:8086/api/v1}"
)
public interface CommunicationOnboardingClient {

    @GetMapping("/communication/internal/announcement-count/{schoolId}")
    Map<String, Long> announcementCount(
            @PathVariable Long schoolId,
            @RequestHeader("X-Internal-Token") String internalToken);
}
