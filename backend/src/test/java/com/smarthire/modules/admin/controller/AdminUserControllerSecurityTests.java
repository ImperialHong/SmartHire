package com.smarthire.modules.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.admin.dto.request.AdminUserStatusUpdateRequest;
import com.smarthire.modules.admin.dto.response.AdminUserResponse;
import com.smarthire.modules.admin.service.AdminUserService;
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
class AdminUserControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean(name = "adminUserServiceImpl")
    private AdminUserService adminUserService;

    @Test
    void listUsersShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void listUsersShouldRejectHrRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(2L, "hr@example.com", "HR User", List.of("HR"))
        );

        mockMvc.perform(get("/api/admin/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void listUsersShouldAllowAdminRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(99L, "admin@example.com", "Admin User", List.of("ADMIN"))
        );

        AdminUserResponse user = new AdminUserResponse(
            1L,
            "candidate@example.com",
            "Candidate User",
            "13800138000",
            "ACTIVE",
            LocalDateTime.of(2026, 3, 14, 10, 0),
            LocalDateTime.of(2026, 3, 1, 10, 0),
            LocalDateTime.of(2026, 3, 14, 10, 0),
            List.of("CANDIDATE")
        );

        when(adminUserService.listUsers(any(AuthenticatedUser.class), any()))
            .thenReturn(PageResponse.of(List.of(user), 1, 10, 1));

        mockMvc.perform(get("/api/admin/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.records[0].email").value("candidate@example.com"))
            .andExpect(jsonPath("$.data.records[0].roles[0]").value("CANDIDATE"));
    }

    @Test
    void updateUserStatusShouldAllowAdminRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(99L, "admin@example.com", "Admin User", List.of("ADMIN"))
        );

        AdminUserResponse user = new AdminUserResponse(
            2L,
            "hr@example.com",
            "HR User",
            "13800138001",
            "DISABLED",
            LocalDateTime.of(2026, 3, 14, 10, 0),
            LocalDateTime.of(2026, 3, 1, 10, 0),
            LocalDateTime.of(2026, 3, 15, 9, 30),
            List.of("HR")
        );

        when(adminUserService.updateUserStatus(any(AuthenticatedUser.class), any(Long.class), any(AdminUserStatusUpdateRequest.class)))
            .thenReturn(user);

        mockMvc.perform(patch("/api/admin/users/2/status")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "DISABLED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("DISABLED"));
    }
}
