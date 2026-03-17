package com.smarthire.modules.notification.messaging;

import com.smarthire.modules.notification.service.NotificationPersistenceService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageListener {

    private final NotificationPersistenceService notificationPersistenceService;

    public NotificationMessageListener(NotificationPersistenceService notificationPersistenceService) {
        this.notificationPersistenceService = notificationPersistenceService;
    }

    @RabbitListener(queues = "${app.messaging.notification.queue:smarthire.notification.persist}")
    public void handleNotification(NotificationMessage message) {
        notificationPersistenceService.persistNotification(message);
    }
}
