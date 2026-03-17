package com.smarthire.modules.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarthire.modules.notification.entity.NotificationEntity;
import com.smarthire.modules.notification.mapper.NotificationMapper;
import com.smarthire.modules.notification.messaging.NotificationMessage;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationPersistenceServiceTests {

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationPersistenceService notificationPersistenceService;

    @Test
    void persistNotificationShouldInsertUnreadNotificationWhenEventIsNew() {
        NotificationMessage message = new NotificationMessage(
            "event-key-1",
            99L,
            "APPLICATION_SUBMITTED",
            "New application received",
            "Candidate User applied for Backend Engineer",
            "APPLICATION",
            200L
        );

        when(notificationMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            NotificationEntity notification = invocation.getArgument(0);
            assertEquals("event-key-1", notification.getEventKey());
            assertEquals(99L, notification.getRecipientUserId());
            assertEquals("APPLICATION_SUBMITTED", notification.getType());
            assertEquals("New application received", notification.getTitle());
            assertEquals("Candidate User applied for Backend Engineer", notification.getContent());
            assertEquals("APPLICATION", notification.getRelatedType());
            assertEquals(200L, notification.getRelatedId());
            assertEquals(false, notification.getReadFlag());
            assertNotNull(notification.getCreatedAt());
            assertNotNull(notification.getUpdatedAt());
            notification.setId(10L);
            return 1;
        }).when(notificationMapper).insert(any(NotificationEntity.class));

        notificationPersistenceService.persistNotification(message);

        verify(notificationMapper).insert(any(NotificationEntity.class));
    }

    @Test
    void persistNotificationShouldSkipInsertWhenEventAlreadyExists() {
        NotificationMessage message = new NotificationMessage(
            "event-key-1",
            99L,
            "APPLICATION_SUBMITTED",
            "New application received",
            "Candidate User applied for Backend Engineer",
            "APPLICATION",
            200L
        );
        NotificationEntity existing = new NotificationEntity();
        existing.setId(10L);
        existing.setEventKey("event-key-1");
        existing.setCreatedAt(LocalDateTime.now());

        when(notificationMapper.selectOne(any())).thenReturn(existing);

        notificationPersistenceService.persistNotification(message);

        verify(notificationMapper, never()).insert(any(NotificationEntity.class));
    }
}
