package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.usecase.GetSchoolStatsUseCase;
import br.com.uebiescola.core.domain.projection.GrowthStatsProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/schools/stats")
@RequiredArgsConstructor
public class SchoolStatsController {

    private final GetSchoolStatsUseCase statsUseCase;

    @GetMapping("/plans")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<Map<String, Long>> getStatsByPlan() {
        return ResponseEntity.ok(statsUseCase.getSchoolsCountByPlan());
    }

    @GetMapping("/growth")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<List<GrowthStatsProjection>> getGrowth() {
        return ResponseEntity.ok(statsUseCase.getGrowthStats());
    }
}