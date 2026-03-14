package com.smarthire.modules.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.application.dto.request.ApplicationCreateRequest;
import com.smarthire.modules.application.dto.request.ApplicationListRequest;
import com.smarthire.modules.application.dto.request.ApplicationStatusUpdateRequest;
import com.smarthire.modules.application.dto.response.ApplicationResponse;
import com.smarthire.modules.application.service.ApplicationService;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.auth.security.JwtTokenService;
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
class ApplicationControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean(name = "applicationServiceImpl")
    private ApplicationService applicationService;

    @Test
    void applyShouldAllowCandidateRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(1L, "candidate@example.com", "Candidate User", List.of("CANDIDATE"))
        );

        ApplicationResponse response = new ApplicationResponse(
            101L,
            88L,
            "Backend Engineer",
            "Shanghai",
            "Engineering",
            1L,
            null,
            null,
            "APPLIED",
            "/files/resume.pdf",
            "I am interested in this role",
            null,
            LocalDateTime.of(2026, 3, 14, 23, 0),
            LocalDateTime.of(2026, 3, 14, 23, 0),
            null,
            LocalDateTime.of(2026, 3, 14, 23, 0),
            LocalDateTime.of(2026, 3, 14, 23, 0)
        );

        when(applicationService.apply(any(AuthenticatedUser.class), any(ApplicationCreateRequest.class)))
            .thenReturn(response);

        mockMvc.perform(post("/api/applications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "jobId": 88,
                      "resumeFilePath": "/files/resume.pdf",
                      "coverLetter": "I am interested in this role"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(101))
            .andExpect(jsonPath("$.data.status").value("APPLIED"));
    }

    @Test
    void applyShouldRejectHrRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(2L, "hr@example.com", "HR User", List.of("HR"))
        );

        mockMvc.perform(post("/api/applications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "jobId": 88
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void listMyApplicationsShouldAllowCandidateRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(1L, "candidate@example.com", "Candidate User", List.of("CANDIDATE"))
        );

        when(applicationService.listMyApplications(any(AuthenticatedUser.class), any(ApplicationListRequest.class)))
            .thenReturn(PageResponse.of(List.of(), 1, 10, 0));

        mockMvc.perform(get("/api/applications/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listJobApplicationsShouldAllowHrRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(2L, "hr@example.com", "HR User", List.of("HR"))
        );

        when(applicationService.listJobApplications(
            any(AuthenticatedUser.class),
            any(Long.class),
            any(ApplicationListRequest.class)
        )).thenReturn(PageResponse.of(List.of(), 1, 10, 0));

        mockMvc.perform(get("/api/jobs/88/applications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listJobApplicationsShouldRejectCandidateRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(1L, "candidate@example.com", "Candidate User", List.of("CANDIDATE"))
        );

        mockMvc.perform(get("/api/jobs/88/applications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void updateStatusShouldAllowHrRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(2L, "hr@example.com", "HR User", List.of("HR"))
        );

        ApplicationResponse response = new ApplicationResponse(
            101L,
            88L,
            "Backend Engineer",
            "Shanghai",
            "Engineering",
            1L,
            "Candidate User",
            "candidate@example.com",
            "REVIEWING",
            "/files/resume.pdf",
            "I am interested in this role",
            "Looks promising",
            LocalDateTime.of(2026, 3, 14, 23, 0),
            LocalDateTime.of(2026, 3, 15, 10, 0),
            2L,
            LocalDateTime.of(2026, 3, 14, 23, 0),
            LocalDateTime.of(2026, 3, 15, 10, 0)
        );

        when(applicationService.updateStatus(
            any(AuthenticatedUser.class),
            any(Long.class),
            any(ApplicationStatusUpdateRequest.class)
        )).thenReturn(response);

        mockMvc.perform(patch("/api/applications/101/status")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "REVIEWING",
                      "hrNote": "Looks promising"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("REVIEWING"));
    }
}
