package com.smarthire.modules.notification.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarthire.common.api.PageResponse;
import com.smarthire.common.exception.BusinessException;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.notification.dto.request.NotificationListRequest;
import com.smarthire.modules.notification.dto.response.NotificationResponse;
import com.smarthire.modules.notification.entity.NotificationEntity;
import com.smarthire.modules.notification.mapper.NotificationMapper;
import com.smarthire.modules.notification.messaging.NotificationEventKeyFactory;
import com.smarthire.modules.notification.messaging.NotificationMessage;
import com.smarthire.modules.notification.messaging.NotificationMessagePublisher;
import com.smarthire.modules.notification.service.NotificationPersistenceService;
import com.smarthire.modules.notification.service.NotificationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private static final Set<String> NOTIFICATION_TYPES = Set.of(
        "APPLICATION_SUBMITTED",
        "APPLICATION_STATUS_CHANGED",
        "INTERVIEW_SCHEDULED",
        "INTERVIEW_UPDATED",
        "SYSTEM"
    );

    private final NotificationMapper notificationMapper;

    private final NotificationMessagePublisher notificationMessagePublisher;

    private final NotificationPersistenceService notificationPersistenceService;

    public NotificationServiceImpl(
        NotificationMapper notificationMapper,
        NotificationMessagePublisher notificationMessagePublisher,
        NotificationPersistenceService notificationPersistenceService
    ) {
        this.notificationMapper = notificationMapper;
        this.notificationMessagePublisher = notificationMessagePublisher;
        this.notificationPersistenceService = notificationPersistenceService;
    }

    @Override
    public PageResponse<NotificationResponse> listMyNotifications(
        AuthenticatedUser currentUser,
        NotificationListRequest request
    ) {
        validateAuthenticated(currentUser);

        var query = Wrappers.<NotificationEntity>lambdaQuery()
            .eq(NotificationEntity::getRecipientUserId, currentUser.userId())
            .orderByDesc(NotificationEntity::getCreatedAt);
        if (request.isRead() != null) {
            query.eq(NotificationEntity::getReadFlag, request.isRead());
        }

        Page<NotificationEntity> pageRequest = new Page<>(request.page(), request.size());
        Page<NotificationEntity> page = notificationMapper.selectPage(pageRequest, query);

        List<NotificationResponse> records = page.getRecords().stream()
            .map(NotificationResponse::fromEntity)
            .toList();

        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    public long countUnreadNotifications(AuthenticatedUser currentUser) {
        validateAuthenticated(currentUser);

        return notificationMapper.selectCount(
            Wrappers.<NotificationEntity>lambdaQuery()
                .eq(NotificationEntity::getRecipientUserId, currentUser.userId())
                .eq(NotificationEntity::getReadFlag, false)
        );
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(AuthenticatedUser currentUser, Long notificationId) {
        validateAuthenticated(currentUser);

        NotificationEntity notification = getOwnedNotification(currentUser, notificationId);
        if (Boolean.TRUE.equals(notification.getReadFlag())) {
            return NotificationResponse.fromEntity(notification);
        }

        LocalDateTime now = LocalDateTime.now();
        notification.setReadFlag(true);
        notification.setReadAt(now);
        notification.setUpdatedAt(now);
        notificationMapper.updateById(notification);

        return NotificationResponse.fromEntity(notification);
    }

    @Override
    @Transactional
    public void createNotification(
        Long recipientUserId,
        String type,
        String title,
        String content,
        String relatedType,
        Long relatedId
    ) {
        if (recipientUserId == null) {
            return;
        }
        if (!NOTIFICATION_TYPES.contains(type)) {
            throw new BusinessException("INVALID_NOTIFICATION_TYPE", "Invalid notification type");
        }

        String normalizedTitle = trimRequired(title, "Notification title is required");
        String normalizedContent = trimRequired(content, "Notification content is required");
        String normalizedRelatedType = trimToNull(relatedType);
        NotificationMessage message = new NotificationMessage(
            NotificationEventKeyFactory.generate(
                recipientUserId,
                type,
                normalizedTitle,
                normalizedContent,
                normalizedRelatedType,
                relatedId
            ),
            recipientUserId,
            type,
            normalizedTitle,
            normalizedContent,
            normalizedRelatedType,
            relatedId
        );

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatchNotificationMessage(message);
                }
            });
            return;
        }

        dispatchNotificationMessage(message);
    }

    private NotificationEntity getOwnedNotification(AuthenticatedUser currentUser, Long notificationId) {
        NotificationEntity notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException("NOTIFICATION_NOT_FOUND", "Notification was not found");
        }
        if (!currentUser.userId().equals(notification.getRecipientUserId())) {
            throw new AccessDeniedException("You can only manage your own notifications");
        }
        return notification;
    }

    private void validateAuthenticated(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.userId() == null) {
            throw new AccessDeniedException("Authentication is required");
        }
    }

    private String trimRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("INVALID_NOTIFICATION", message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void dispatchNotificationMessage(NotificationMessage message) {
        try {
            notificationMessagePublisher.publish(message);
        } catch (AmqpException exception) {
            log.warn(
                "Failed to publish notification event {}, falling back to direct persistence",
                message.eventKey(),
                exception
            );
            notificationPersistenceService.persistNotification(message);
        }
    }
}
