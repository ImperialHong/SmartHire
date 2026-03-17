package com.smarthire.modules.notification.messaging;

public record NotificationMessage(
    String eventKey,
    Long recipientUserId,
    String type,
    String title,
    String content,
    String relatedType,
    Long relatedId
) {
}
