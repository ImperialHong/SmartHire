package com.smarthire.modules.notification.messaging;

import com.smarthire.modules.notification.config.NotificationMessagingProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    private final NotificationMessagingProperties messagingProperties;

    public NotificationMessagePublisher(
        RabbitTemplate rabbitTemplate,
        NotificationMessagingProperties messagingProperties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.messagingProperties = messagingProperties;
    }

    public void publish(NotificationMessage message) {
        rabbitTemplate.convertAndSend(
            messagingProperties.getExchange(),
            routingKeyFor(message.type()),
            message
        );
    }

    private String routingKeyFor(String type) {
        return switch (type) {
            case "APPLICATION_SUBMITTED" -> "application.submitted";
            case "APPLICATION_STATUS_CHANGED" -> "application.status.changed";
            case "INTERVIEW_SCHEDULED" -> "interview.scheduled";
            case "INTERVIEW_UPDATED" -> "interview.updated";
            case "SYSTEM" -> "system.general";
            default -> throw new IllegalArgumentException("Unsupported notification type: " + type);
        };
    }
}
