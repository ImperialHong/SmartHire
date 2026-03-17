package com.smarthire.modules.notification.dto.response;

import com.smarthire.modules.notification.entity.NotificationEntity;
import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    String type,
    String title,
    String content,
    String relatedType,
    Long relatedId,
    Boolean isRead,
    LocalDateTime readAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static NotificationResponse fromEntity(NotificationEntity entity) {
        return new NotificationResponse(
            entity.getId(),
            entity.getType(),
            entity.getTitle(),
            entity.getContent(),
            entity.getRelatedType(),
            entity.getRelatedId(),
            entity.getReadFlag(),
            entity.getReadAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
