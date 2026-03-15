package com.smarthire.modules.statistics.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.auth.security.JwtTokenService;
import com.smarthire.modules.statistics.dto.response.StatisticsOverviewResponse;
import com.smarthire.modules.statistics.service.StatisticsService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class StatisticsControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean(name = "statisticsServiceImpl")
    private StatisticsService statisticsService;

    @Test
    void overviewShouldRejectCandidateRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(1L, "candidate@example.com", "Candidate User", List.of("CANDIDATE"))
        );

        mockMvc.perform(get("/api/statistics/overview")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void overviewShouldAllowHrRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(2L, "hr@example.com", "HR User", List.of("HR"))
        );

        LinkedHashMap<String, Long> jobStatusCounts = new LinkedHashMap<>();
        jobStatusCounts.put("OPEN", 2L);
        jobStatusCounts.put("CLOSED", 1L);
        jobStatusCounts.put("EXPIRED", 0L);

        LinkedHashMap<String, Long> applicationStatusCounts = new LinkedHashMap<>();
        applicationStatusCounts.put("APPLIED", 1L);
        applicationStatusCounts.put("REVIEWING", 1L);
        applicationStatusCounts.put("INTERVIEW", 1L);
        applicationStatusCounts.put("OFFERED", 0L);
        applicationStatusCounts.put("REJECTED", 0L);

        LinkedHashMap<String, Long> interviewStatusCounts = new LinkedHashMap<>();
        interviewStatusCounts.put("SCHEDULED", 1L);
        interviewStatusCounts.put("COMPLETED", 0L);
        interviewStatusCounts.put("CANCELLED", 0L);

        LinkedHashMap<String, Long> interviewResultCounts = new LinkedHashMap<>();
        interviewResultCounts.put("PENDING", 1L);
        interviewResultCounts.put("PASSED", 0L);
        interviewResultCounts.put("FAILED", 0L);

        when(statisticsService.getOverview(any(AuthenticatedUser.class))).thenReturn(
            new StatisticsOverviewResponse(
                "HR",
                2L,
                new StatisticsOverviewResponse.JobStatistics(3L, jobStatusCounts),
                new StatisticsOverviewResponse.ApplicationStatistics(3L, 2L, applicationStatusCounts),
                new StatisticsOverviewResponse.InterviewStatistics(1L, 1L, interviewStatusCounts, interviewResultCounts),
                LocalDateTime.of(2026, 3, 15, 10, 30)
            )
        );

        mockMvc.perform(get("/api/statistics/overview")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.scope").value("HR"))
            .andExpect(jsonPath("$.data.ownerUserId").value(2))
            .andExpect(jsonPath("$.data.jobs.total").value(3))
            .andExpect(jsonPath("$.data.applications.withResume").value(2))
            .andExpect(jsonPath("$.data.interviews.upcoming").value(1));
    }
}
