package com.smarthire.modules.notification.controller;

import com.smarthire.common.api.ApiResponse;
import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.notification.dto.request.NotificationListRequest;
import com.smarthire.modules.notification.dto.response.NotificationResponse;
import com.smarthire.modules.notification.dto.response.NotificationUnreadCountResponse;
import com.smarthire.modules.notification.service.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> listMyNotifications(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
        @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0")
        @Max(value = 100, message = "size must be less than or equal to 100") long size,
        @RequestParam(required = false) Boolean isRead
    ) {
        return ApiResponse.success(notificationService.listMyNotifications(
            currentUser,
            new NotificationListRequest(page, size, isRead)
        ));
    }

    @GetMapping("/unread-count")
    public ApiResponse<NotificationUnreadCountResponse> unreadCount(
        @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(new NotificationUnreadCountResponse(
            notificationService.countUnreadNotifications(currentUser)
        ));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markAsRead(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable Long id
    ) {
        return ApiResponse.success(
            "Notification marked as read",
            notificationService.markAsRead(currentUser, id)
        );
    }
}
