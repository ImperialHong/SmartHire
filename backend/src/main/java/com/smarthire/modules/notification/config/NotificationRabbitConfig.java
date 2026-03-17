package com.smarthire.modules.notification.config;

import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class NotificationRabbitConfig {

    @Bean
    public TopicExchange notificationExchange(NotificationMessagingProperties properties) {
        return new TopicExchange(properties.getExchange(), true, false);
    }

    @Bean
    public TopicExchange notificationDeadLetterExchange(NotificationMessagingProperties properties) {
        return new TopicExchange(properties.getDeadLetterExchange(), true, false);
    }

    @Bean
    public Queue notificationQueue(NotificationMessagingProperties properties) {
        return QueueBuilder.durable(properties.getQueue())
            .withArguments(
                Map.of(
                    "x-dead-letter-exchange",
                    properties.getDeadLetterExchange(),
                    "x-dead-letter-routing-key",
                    properties.getQueue()
                )
            )
            .build();
    }

    @Bean
    public Queue notificationDeadLetterQueue(NotificationMessagingProperties properties) {
        return QueueBuilder.durable(properties.getDeadLetterQueue()).build();
    }

    @Bean
    public Binding notificationQueueBinding(
        @Qualifier("notificationQueue") Queue notificationQueue,
        @Qualifier("notificationExchange") TopicExchange notificationExchange
    ) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with("#");
    }

    @Bean
    public Binding notificationDeadLetterQueueBinding(
        @Qualifier("notificationDeadLetterQueue") Queue notificationDeadLetterQueue,
        @Qualifier("notificationDeadLetterExchange") TopicExchange notificationDeadLetterExchange,
        NotificationMessagingProperties properties
    ) {
        return BindingBuilder.bind(notificationDeadLetterQueue)
            .to(notificationDeadLetterExchange)
            .with(properties.getQueue());
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplateCustomizer rabbitTemplateCustomizer(MessageConverter rabbitMessageConverter) {
        return rabbitTemplate -> rabbitTemplate.setMessageConverter(rabbitMessageConverter);
    }
}
