package com.smarthire.modules.interview.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.auth.security.JwtTokenService;
import com.smarthire.modules.interview.dto.request.InterviewCreateRequest;
import com.smarthire.modules.interview.dto.request.InterviewListRequest;
import com.smarthire.modules.interview.dto.request.InterviewUpdateRequest;
import com.smarthire.modules.interview.dto.response.InterviewResponse;
import com.smarthire.modules.interview.service.InterviewService;
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
class InterviewControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean(name = "interviewServiceImpl")
    private InterviewService interviewService;

    @Test
    void scheduleInterviewShouldAllowHrRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(2L, "hr@example.com", "HR User", List.of("HR"))
        );

        InterviewResponse response = buildResponse("SCHEDULED", "PENDING");
        when(interviewService.scheduleInterview(any(AuthenticatedUser.class), any(InterviewCreateRequest.class)))
            .thenReturn(response);

        mockMvc.perform(post("/api/interviews")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationId": 200,
                      "interviewAt": "2026-03-20T15:00:00",
                      "location": "Room B",
                      "meetingLink": "https://meet.example.com/new",
                      "remark": "Please arrive early"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("SCHEDULED"));
    }

    @Test
    void scheduleInterviewShouldRejectCandidateRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(1L, "candidate@example.com", "Candidate User", List.of("CANDIDATE"))
        );

        mockMvc.perform(post("/api/interviews")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationId": 200,
                      "interviewAt": "2026-03-20T15:00:00"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void listMyInterviewsShouldAllowCandidateRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(1L, "candidate@example.com", "Candidate User", List.of("CANDIDATE"))
        );

        when(interviewService.listMyInterviews(any(AuthenticatedUser.class), any(InterviewListRequest.class)))
            .thenReturn(PageResponse.of(List.of(), 1, 10, 0));

        mockMvc.perform(get("/api/interviews/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listJobInterviewsShouldAllowHrRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(2L, "hr@example.com", "HR User", List.of("HR"))
        );

        when(interviewService.listJobInterviews(
            any(AuthenticatedUser.class),
            any(Long.class),
            any(InterviewListRequest.class)
        )).thenReturn(PageResponse.of(List.of(), 1, 10, 0));

        mockMvc.perform(get("/api/jobs/100/interviews")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateInterviewShouldAllowHrRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(2L, "hr@example.com", "HR User", List.of("HR"))
        );

        when(interviewService.updateInterview(
            any(AuthenticatedUser.class),
            any(Long.class),
            any(InterviewUpdateRequest.class)
        )).thenReturn(buildResponse("COMPLETED", "PASSED"));

        mockMvc.perform(patch("/api/interviews/300")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "COMPLETED",
                      "result": "PASSED",
                      "remark": "Strong communication"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.result").value("PASSED"));
    }

    private InterviewResponse buildResponse(String status, String result) {
        return new InterviewResponse(
            300L,
            200L,
            100L,
            "Backend Engineer",
            1L,
            "Candidate User",
            "candidate@example.com",
            "INTERVIEW",
            2L,
            LocalDateTime.of(2026, 3, 20, 15, 0),
            "Room B",
            "https://meet.example.com/new",
            status,
            result,
            "Strong communication",
            LocalDateTime.of(2026, 3, 15, 9, 0),
            LocalDateTime.of(2026, 3, 15, 10, 0)
        );
    }
}
