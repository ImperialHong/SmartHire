package com.smarthire.modules.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.auth.security.JwtTokenService;
import com.smarthire.modules.notification.dto.request.NotificationListRequest;
import com.smarthire.modules.notification.dto.response.NotificationResponse;
import com.smarthire.modules.notification.service.NotificationService;
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
class NotificationControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean(name = "notificationServiceImpl")
    private NotificationService notificationService;

    @Test
    void listNotificationsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/notifications"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void listNotificationsShouldAllowAuthenticatedUser() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(1L, "candidate@example.com", "Candidate User", List.of("CANDIDATE"))
        );

        NotificationResponse notification = new NotificationResponse(
            10L,
            "APPLICATION_STATUS_CHANGED",
            "Application status updated",
            "Your application is now REVIEWING",
            "APPLICATION",
            200L,
            false,
            null,
            LocalDateTime.of(2026, 3, 16, 12, 0),
            LocalDateTime.of(2026, 3, 16, 12, 0)
        );

        when(notificationService.listMyNotifications(any(AuthenticatedUser.class), any(NotificationListRequest.class)))
            .thenReturn(PageResponse.of(List.of(notification), 1, 10, 1));

        mockMvc.perform(get("/api/notifications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.records[0].type").value("APPLICATION_STATUS_CHANGED"));
    }

    @Test
    void unreadCountShouldAllowAuthenticatedUser() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(1L, "candidate@example.com", "Candidate User", List.of("CANDIDATE"))
        );

        when(notificationService.countUnreadNotifications(any(AuthenticatedUser.class))).thenReturn(3L);

        mockMvc.perform(get("/api/notifications/unread-count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.unreadCount").value(3));
    }

    @Test
    void markAsReadShouldAllowAuthenticatedUser() throws Exception {
        String token = jwtTokenService.generateToken(
            new AuthenticatedUser(1L, "candidate@example.com", "Candidate User", List.of("CANDIDATE"))
        );

        NotificationResponse notification = new NotificationResponse(
            10L,
            "APPLICATION_STATUS_CHANGED",
            "Application status updated",
            "Your application is now REVIEWING",
            "APPLICATION",
            200L,
            true,
            LocalDateTime.of(2026, 3, 16, 12, 5),
            LocalDateTime.of(2026, 3, 16, 12, 0),
            LocalDateTime.of(2026, 3, 16, 12, 5)
        );

        when(notificationService.markAsRead(any(AuthenticatedUser.class), any(Long.class)))
            .thenReturn(notification);

        mockMvc.perform(patch("/api/notifications/10/read")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.isRead").value(true));
    }
}
