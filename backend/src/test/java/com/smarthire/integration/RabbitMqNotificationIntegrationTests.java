package com.smarthire.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarthire.modules.notification.config.NotificationMessagingProperties;
import com.smarthire.modules.notification.messaging.NotificationMessage;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RabbitMqNotificationIntegrationTests extends ContainersIntegrationTestSupport {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificationMessagingProperties notificationMessagingProperties;

    @Test
    void rabbitListenerShouldPersistNotificationAndDeduplicateByEventKey() {
        Long candidateUserId = insertUser("mq-candidate@example.com", "MQ Candidate", "ACTIVE");
        assignRole(candidateUserId, "CANDIDATE");

        NotificationMessage message = new NotificationMessage(
            "event-rabbitmq-it-001",
            candidateUserId,
            "SYSTEM",
            "Integration Notification",
            "Persisted through RabbitMQ",
            "SYSTEM_TEST",
            99L
        );

        rabbitTemplate.convertAndSend(
            notificationMessagingProperties.getExchange(),
            "system.general",
            message
        );

        waitForAssertion(Duration.ofSeconds(10), () -> assertThat(countNotifications(message.eventKey())).isEqualTo(1));

        rabbitTemplate.convertAndSend(
            notificationMessagingProperties.getExchange(),
            "system.general",
            message
        );

        waitForAssertion(Duration.ofSeconds(10), () -> assertThat(countNotifications(message.eventKey())).isEqualTo(1));
    }

    private int countNotifications(String eventKey) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE event_key = ?",
            Integer.class,
            eventKey
        );
        return count == null ? 0 : count;
    }
}
