package com.smarthire.modules.operationlog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.auth.security.JwtTokenService;
import com.smarthire.modules.operationlog.dto.response.OperationLogResponse;
import com.smarthire.modules.operationlog.service.OperationLogService;
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
class OperationLogControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean(name = "operationLogServiceImpl")
    private OperationLogService operationLogService;

    @Test
    void listLogsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/operation-logs"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void listLogsShouldRejectHrRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(2L, "hr@example.com", "HR User", List.of("HR"))
        );

        mockMvc.perform(get("/api/admin/operation-logs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void listLogsShouldAllowAdminRole() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(99L, "admin@example.com", "Admin User", List.of("ADMIN"))
        );

        OperationLogResponse log = new OperationLogResponse(
            1L,
            99L,
            "admin@example.com",
            "Admin User",
            List.of("ADMIN"),
            "USER_STATUS_UPDATED",
            "USER",
            2L,
            "Updated user status to DISABLED",
            LocalDateTime.of(2026, 3, 15, 16, 0)
        );

        when(operationLogService.listLogs(any(AuthenticatedUser.class), any()))
            .thenReturn(PageResponse.of(List.of(log), 1, 10, 1));

        mockMvc.perform(get("/api/admin/operation-logs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.records[0].action").value("USER_STATUS_UPDATED"))
            .andExpect(jsonPath("$.data.records[0].targetType").value("USER"));
    }
}
