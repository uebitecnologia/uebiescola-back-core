package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.service.TokenService;
import br.com.uebiescola.core.infrastructure.security.SecurityFilter;
import br.com.uebiescola.core.application.usecase.GetSchoolStatsUseCase;
import br.com.uebiescola.core.domain.projection.GrowthStatsProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SchoolStatsController.class)
@AutoConfigureMockMvc(addFilters = false)
class SchoolStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private SecurityFilter securityFilter;

    @MockBean
    private GetSchoolStatsUseCase statsUseCase;

    @Test
    void getStatsByPlan_shouldReturn200() throws Exception {
        Map<String, Long> planStats = Map.of(
                "BASIC", 10L,
                "PREMIUM", 5L,
                "ENTERPRISE", 2L
        );
        when(statsUseCase.getSchoolsCountByPlan()).thenReturn(planStats);

        mockMvc.perform(get("/api/v1/schools/stats/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.BASIC").value(10))
                .andExpect(jsonPath("$.PREMIUM").value(5))
                .andExpect(jsonPath("$.ENTERPRISE").value(2));
    }

    @Test
    void getStatsByPlan_emptyResults_shouldReturn200() throws Exception {
        when(statsUseCase.getSchoolsCountByPlan()).thenReturn(Map.of());

        mockMvc.perform(get("/api/v1/schools/stats/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());
    }

    @Test
    void getGrowth_shouldReturn200() throws Exception {
        GrowthStatsProjection projection1 = new GrowthStatsProjection() {
            @Override public String getMonth() { return "Jan/26"; }
            @Override public Long getTotal() { return 5L; }
        };
        GrowthStatsProjection projection2 = new GrowthStatsProjection() {
            @Override public String getMonth() { return "Feb/26"; }
            @Override public Long getTotal() { return 8L; }
        };

        when(statsUseCase.getGrowthStats()).thenReturn(List.of(projection1, projection2));

        mockMvc.perform(get("/api/v1/schools/stats/growth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].month").value("Jan/26"))
                .andExpect(jsonPath("$[0].total").value(5))
                .andExpect(jsonPath("$[1].month").value("Feb/26"))
                .andExpect(jsonPath("$[1].total").value(8));
    }

    @Test
    void getGrowth_emptyResults_shouldReturn200() throws Exception {
        when(statsUseCase.getGrowthStats()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/schools/stats/growth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ===================== Method not allowed tests =====================

    @Test
    void plans_shouldReturn405WhenPost() throws Exception {
        mockMvc.perform(post("/api/v1/schools/stats/plans"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void growth_shouldReturn405WhenPut() throws Exception {
        mockMvc.perform(put("/api/v1/schools/stats/growth"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ===================== Use case interaction tests =====================

    @Test
    void getStatsByPlan_shouldCallUseCaseOnce() throws Exception {
        when(statsUseCase.getSchoolsCountByPlan()).thenReturn(Map.of("BASIC", 1L));

        mockMvc.perform(get("/api/v1/schools/stats/plans"))
                .andExpect(status().isOk());

        verify(statsUseCase, times(1)).getSchoolsCountByPlan();
    }

    @Test
    void getGrowth_shouldCallUseCaseOnce() throws Exception {
        when(statsUseCase.getGrowthStats()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/schools/stats/growth"))
                .andExpect(status().isOk());

        verify(statsUseCase, times(1)).getGrowthStats();
    }

    // ===================== Large dataset test =====================

    @Test
    void getStatsByPlan_withManyPlans_shouldReturn200() throws Exception {
        Map<String, Long> planStats = Map.of(
                "BASIC", 100L,
                "PREMIUM", 50L,
                "ENTERPRISE", 25L,
                "FREE", 200L,
                "TRIAL", 75L
        );
        when(statsUseCase.getSchoolsCountByPlan()).thenReturn(planStats);

        mockMvc.perform(get("/api/v1/schools/stats/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.BASIC").value(100))
                .andExpect(jsonPath("$.FREE").value(200))
                .andExpect(jsonPath("$.TRIAL").value(75));
    }
}
