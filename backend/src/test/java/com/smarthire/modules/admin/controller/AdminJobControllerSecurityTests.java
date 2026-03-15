package com.smarthire.modules.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.admin.dto.response.AdminJobResponse;
import com.smarthire.modules.admin.service.AdminJobService;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.auth.security.JwtTokenService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class AdminJobControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean(name = "adminJobServiceImpl")
    private AdminJobService adminJobService;

    @Test
    void listJobsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/jobs"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void listJobsShouldRejectHrRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(2L, "hr@example.com", "HR User", List.of("HR"))
        );

        mockMvc.perform(get("/api/admin/jobs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void listJobsShouldAllowAdminRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(99L, "admin@example.com", "Admin User", List.of("ADMIN"))
        );

        AdminJobResponse job = new AdminJobResponse(
            3L,
            2L,
            "HR User",
            "hr@example.com",
            "ACTIVE",
            "Backend Engineer",
            "Build workflow services",
            "Shanghai",
            "Engineering",
            "FULL_TIME",
            "MID",
            BigDecimal.valueOf(15000),
            BigDecimal.valueOf(25000),
            "OPEN",
            LocalDateTime.of(2026, 3, 20, 18, 0),
            LocalDateTime.of(2026, 3, 10, 10, 0),
            LocalDateTime.of(2026, 3, 15, 9, 30)
        );

        when(adminJobService.listJobs(any(AuthenticatedUser.class), any()))
            .thenReturn(PageResponse.of(List.of(job), 1, 10, 1));

        mockMvc.perform(get("/api/admin/jobs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.records[0].title").value("Backend Engineer"))
            .andExpect(jsonPath("$.data.records[0].ownerEmail").value("hr@example.com"));
    }
}
