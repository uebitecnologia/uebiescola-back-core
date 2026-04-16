package br.com.uebiescola.core.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "plans-service", url = "${feign.plans-service.url:http://localhost:8082/api/v1}/plans/admin")
public interface PlanClient {

    @GetMapping("/{id}")
    PlanResponseDTO getPlanById(@PathVariable("id") Long id);

    @GetMapping
    List<PlanResponseDTO> getAllPlans();

    // DTO para capturar exatamente o que o MS-Plans envia
    record PlanResponseDTO(
            String name,
            BigDecimal monthlyPrice,
            boolean promotionActive,
            BigDecimal promotionalPrice
    ) {}
}