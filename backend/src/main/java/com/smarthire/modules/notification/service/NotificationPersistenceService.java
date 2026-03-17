package com.smarthire.modules.notification.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smarthire.modules.notification.entity.NotificationEntity;
import com.smarthire.modules.notification.mapper.NotificationMapper;
import com.smarthire.modules.notification.messaging.NotificationMessage;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPersistenceService {

    private final NotificationMapper notificationMapper;

    public NotificationPersistenceService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Transactional
    public void persistNotification(NotificationMessage message) {
        if (message == null || message.recipientUserId() == null) {
            return;
        }

        if (findByEventKey(message.eventKey()) != null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        NotificationEntity notification = new NotificationEntity();
        notification.setEventKey(message.eventKey());
        notification.setRecipientUserId(message.recipientUserId());
        notification.setType(message.type());
        notification.setTitle(message.title());
        notification.setContent(message.content());
        notification.setRelatedType(message.relatedType());
        notification.setRelatedId(message.relatedId());
        notification.setReadFlag(false);
        notification.setCreatedAt(now);
        notification.setUpdatedAt(now);

        try {
            notificationMapper.insert(notification);
        } catch (DuplicateKeyException exception) {
            if (findByEventKey(message.eventKey()) == null) {
                throw exception;
            }
        }
    }

    private NotificationEntity findByEventKey(String eventKey) {
        if (eventKey == null || eventKey.isBlank()) {
            return null;
        }
        return notificationMapper.selectOne(
            Wrappers.<NotificationEntity>lambdaQuery()
                .eq(NotificationEntity::getEventKey, eventKey)
                .last("LIMIT 1")
        );
    }
}
