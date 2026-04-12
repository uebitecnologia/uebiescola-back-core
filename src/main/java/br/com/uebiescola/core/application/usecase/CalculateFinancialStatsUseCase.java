package br.com.uebiescola.core.application.usecase;

import br.com.uebiescola.core.infrastructure.client.PlanClient;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CalculateFinancialStatsUseCase {

    private final JpaSchoolRepository schoolRepository;
    private final PlanClient planClient;

    public BigDecimal calculateMRR() {
        // 1. Busca todos os planos e seus preços atuais no MS-Plans
        var activePlans = planClient.getAllPlans();

        // 2. Busca todas as escolas ativas no MS-Core
        var schools = schoolRepository.findAll().stream()
                .filter(s -> s.getActive())
                .toList();

        // 3. Lógica de Soma Inteligente
        return schools.stream().map(school -> {
            String planName = school.getContract().getPlanBase();

            // Procura o preço do plano da escola na lista vinda do MS-Plans
            return activePlans.stream()
                    .filter(p -> p.name().equalsIgnoreCase(planName))
                    .findFirst()
                    .map(p -> p.promotionActive() ? p.promotionalPrice() : p.monthlyPrice())
                    .orElse(BigDecimal.ZERO);
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
