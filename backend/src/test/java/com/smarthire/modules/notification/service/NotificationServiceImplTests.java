package com.smarthire.modules.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.notification.dto.request.NotificationListRequest;
import com.smarthire.modules.notification.dto.response.NotificationResponse;
import com.smarthire.modules.notification.entity.NotificationEntity;
import com.smarthire.modules.notification.mapper.NotificationMapper;
import com.smarthire.modules.notification.service.impl.NotificationServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTests {

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void createNotificationShouldPersistUnreadNotification() {
        doAnswer(invocation -> {
            NotificationEntity notification = invocation.getArgument(0);
            assertEquals(99L, notification.getRecipientUserId());
            assertEquals("APPLICATION_SUBMITTED", notification.getType());
            assertEquals("New application received", notification.getTitle());
            assertEquals("Candidate User applied for Backend Engineer", notification.getContent());
            assertEquals("APPLICATION", notification.getRelatedType());
            assertEquals(200L, notification.getRelatedId());
            assertEquals(false, notification.getRead());
            assertNotNull(notification.getCreatedAt());
            assertNotNull(notification.getUpdatedAt());
            notification.setId(10L);
            return 1;
        }).when(notificationMapper).insert(any(NotificationEntity.class));

        notificationService.createNotification(
            99L,
            "APPLICATION_SUBMITTED",
            "New application received",
            "Candidate User applied for Backend Engineer",
            "APPLICATION",
            200L
        );

        // Persistence interaction already happened; mapper side-effects confirm entity was built.
    }

    @Test
    void listMyNotificationsShouldReturnPagedRecords() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
            99L,
            "candidate@example.com",
            "Candidate User",
            List.of("CANDIDATE")
        );
        NotificationEntity notification = new NotificationEntity();
        notification.setId(10L);
        notification.setType("APPLICATION_STATUS_CHANGED");
        notification.setTitle("Application status updated");
        notification.setContent("Your application is now REVIEWING");
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.of(2026, 3, 16, 12, 0));
        notification.setUpdatedAt(LocalDateTime.of(2026, 3, 16, 12, 0));

        Page<NotificationEntity> page = new Page<>(1, 10);
        page.setRecords(List.of(notification));
        page.setTotal(1);
        when(notificationMapper.selectPage(any(), any())).thenReturn(page);

        PageResponse<NotificationResponse> response = notificationService.listMyNotifications(
            currentUser,
            new NotificationListRequest(1, 10, false)
        );

        assertEquals(1, response.records().size());
        assertEquals("APPLICATION_STATUS_CHANGED", response.records().get(0).type());
        assertFalse(response.records().get(0).isRead());
    }

    @Test
    void markAsReadShouldUpdateOwnedNotification() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
            99L,
            "candidate@example.com",
            "Candidate User",
            List.of("CANDIDATE")
        );
        NotificationEntity notification = new NotificationEntity();
        notification.setId(10L);
        notification.setRecipientUserId(99L);
        notification.setRead(false);

        when(notificationMapper.selectById(10L)).thenReturn(notification);

        NotificationResponse response = notificationService.markAsRead(currentUser, 10L);

        assertEquals(true, response.isRead());
        assertNotNull(response.readAt());
    }

    @Test
    void markAsReadShouldRejectOtherUsersNotification() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
            99L,
            "candidate@example.com",
            "Candidate User",
            List.of("CANDIDATE")
        );
        NotificationEntity notification = new NotificationEntity();
        notification.setId(10L);
        notification.setRecipientUserId(100L);
        notification.setRead(false);

        when(notificationMapper.selectById(10L)).thenReturn(notification);

        assertThrows(AccessDeniedException.class, () -> notificationService.markAsRead(currentUser, 10L));
    }
}
