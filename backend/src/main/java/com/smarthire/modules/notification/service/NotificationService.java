package com.smarthire.modules.notification.service;

import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.notification.dto.request.NotificationListRequest;
import com.smarthire.modules.notification.dto.response.NotificationResponse;

public interface NotificationService {

    PageResponse<NotificationResponse> listMyNotifications(
        AuthenticatedUser currentUser,
        NotificationListRequest request
    );

    long countUnreadNotifications(AuthenticatedUser currentUser);

    NotificationResponse markAsRead(AuthenticatedUser currentUser, Long notificationId);

    void createNotification(
        Long recipientUserId,
        String type,
        String title,
        String content,
        String relatedType,
        Long relatedId
    );
}
