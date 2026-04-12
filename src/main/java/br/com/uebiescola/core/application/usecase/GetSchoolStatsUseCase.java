package br.com.uebiescola.core.application.usecase;

import br.com.uebiescola.core.domain.projection.GrowthStatsProjection;
import br.com.uebiescola.core.infrastructure.persistence.repository.JpaSchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetSchoolStatsUseCase {

    private final JpaSchoolRepository repository;

    @Transactional(readOnly = true)
    public Map<String, Long> getSchoolsCountByPlan() {
        List<Object[]> results = repository.countSchoolsByPlan();

        return results.stream().collect(Collectors.toMap(
                result -> (String) result[0],  // planBase
                result -> (Long) result[1]     // COUNT
        ));
    }

    @Transactional(readOnly = true)
    public List<GrowthStatsProjection> getGrowthStats() {
        return repository.getGrowthStats();
    }
}