package br.com.uebiescola.core.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@FeignClient(name = "finance-service", url = "${feign.finance-service.url:http://localhost:8083/api/v1}")
public interface FinanceClient {

    @GetMapping("/guardians")
    List<Map<String, Object>> getGuardians(@RequestHeader("Authorization") String token, @RequestHeader("X-School-Id") Long schoolId);

    @GetMapping("/finance/invoices")
    List<Map<String, Object>> getInvoices(@RequestHeader("Authorization") String token, @RequestHeader("X-School-Id") Long schoolId);

    @GetMapping("/finance/expenses")
    List<Map<String, Object>> getExpenses(@RequestHeader("Authorization") String token, @RequestHeader("X-School-Id") Long schoolId);

    @GetMapping("/finance/suppliers")
    List<Map<String, Object>> getSuppliers(@RequestHeader("Authorization") String token, @RequestHeader("X-School-Id") Long schoolId);
}
