package com.smarthire.modules.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.notification.dto.request.NotificationListRequest;
import com.smarthire.modules.notification.dto.response.NotificationResponse;
import com.smarthire.modules.notification.entity.NotificationEntity;
import com.smarthire.modules.notification.mapper.NotificationMapper;
import com.smarthire.modules.notification.messaging.NotificationMessage;
import com.smarthire.modules.notification.messaging.NotificationMessagePublisher;
import com.smarthire.modules.notification.service.NotificationPersistenceService;
import com.smarthire.modules.notification.service.impl.NotificationServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTests {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private NotificationMessagePublisher notificationMessagePublisher;

    @Mock
    private NotificationPersistenceService notificationPersistenceService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void createNotificationShouldPublishNotificationEvent() {
        notificationService.createNotification(
            99L,
            "APPLICATION_SUBMITTED",
            "New application received",
            "Candidate User applied for Backend Engineer",
            "APPLICATION",
            200L
        );

        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationMessagePublisher).publish(messageCaptor.capture());
        NotificationMessage message = messageCaptor.getValue();
        assertEquals(99L, message.recipientUserId());
        assertEquals("APPLICATION_SUBMITTED", message.type());
        assertEquals("New application received", message.title());
        assertEquals("Candidate User applied for Backend Engineer", message.content());
        assertEquals("APPLICATION", message.relatedType());
        assertEquals(200L, message.relatedId());
        assertNotNull(message.eventKey());
    }

    @Test
    void createNotificationShouldFallbackToDirectPersistenceWhenPublisherFails() {
        doThrow(new AmqpConnectException(new RuntimeException("RabbitMQ unavailable")))
            .when(notificationMessagePublisher)
            .publish(any(NotificationMessage.class));

        notificationService.createNotification(
            99L,
            "APPLICATION_SUBMITTED",
            "New application received",
            "Candidate User applied for Backend Engineer",
            "APPLICATION",
            200L
        );

        verify(notificationPersistenceService).persistNotification(any(NotificationMessage.class));
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
        notification.setReadFlag(false);
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
        notification.setReadFlag(false);

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
        notification.setReadFlag(false);

        when(notificationMapper.selectById(10L)).thenReturn(notification);

        assertThrows(AccessDeniedException.class, () -> notificationService.markAsRead(currentUser, 10L));
    }
}
