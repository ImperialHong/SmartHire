package com.smarthire.modules.job.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.auth.security.JwtTokenService;
import com.smarthire.modules.job.dto.request.JobCreateRequest;
import com.smarthire.modules.job.dto.response.JobResponse;
import com.smarthire.modules.job.service.JobService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class JobControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean(name = "jobServiceImpl")
    private JobService jobService;

    @Test
    void listJobsShouldBePublic() throws Exception {
        when(jobService.listJobs(any())).thenReturn(PageResponse.of(List.of(), 1, 10, 0));

        mockMvc.perform(get("/api/jobs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createJobShouldRejectCandidateRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(1L, "candidate@example.com", "Candidate User", List.of("CANDIDATE"))
        );

        mockMvc.perform(post("/api/jobs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Backend Engineer",
                      "description": "Build hiring platform modules"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void createJobShouldAllowHrRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(2L, "hr@example.com", "HR User", List.of("HR"))
        );

        JobResponse response = new JobResponse(
            100L,
            2L,
            "Backend Engineer",
            "Build hiring platform modules",
            "Shanghai",
            "Engineering",
            "FULL_TIME",
            "MID",
            new BigDecimal("15000"),
            new BigDecimal("25000"),
            "OPEN",
            LocalDateTime.of(2026, 4, 1, 18, 0),
            LocalDateTime.of(2026, 3, 14, 22, 0),
            LocalDateTime.of(2026, 3, 14, 22, 0)
        );

        when(jobService.createJob(any(AuthenticatedUser.class), any(JobCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/jobs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Backend Engineer",
                      "description": "Build hiring platform modules",
                      "city": "Shanghai",
                      "category": "Engineering",
                      "employmentType": "FULL_TIME",
                      "experienceLevel": "MID",
                      "salaryMin": 15000,
                      "salaryMax": 25000,
                      "status": "OPEN",
                      "applicationDeadline": "2026-04-01T18:00:00"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(100))
            .andExpect(jsonPath("$.data.createdBy").value(2));
    }
}
