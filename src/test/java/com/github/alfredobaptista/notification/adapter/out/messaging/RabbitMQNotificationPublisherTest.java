package com.github.alfredobaptista.notification.adapter.out.messaging;

import com.github.alfredobaptista.notification.adapter.out.messaging.routing.EmailRoutingStrategy;
import com.github.alfredobaptista.notification.adapter.out.messaging.routing.NotificationRoutingStrategy;
import com.github.alfredobaptista.notification.adapter.out.messaging.routing.PushRoutingStrategy;
import com.github.alfredobaptista.notification.adapter.out.messaging.routing.SmsRoutingStrategy;
import com.github.alfredobaptista.notification.domain.entity.Notification;
import com.github.alfredobaptista.notification.domain.enums.NotificationChannel;
import com.github.alfredobaptista.notification.domain.enums.NotificationPriority;
import com.github.alfredobaptista.notification.infrastructure.config.RabbitMQConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMQNotificationPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitMQNotificationPublisher publisher;

    @BeforeEach
    void setUp() {

        List<NotificationRoutingStrategy> strategies = List.of(
                new EmailRoutingStrategy(),
                new SmsRoutingStrategy(),
                new PushRoutingStrategy()
        );

        publisher = new RabbitMQNotificationPublisher(
                rabbitTemplate,
                strategies
        );
    }

    @Test
    void shouldPublishEmailNotificationUsingEmailRoutingKey() {

        // Arrange
        Notification notification = createNotification(
                NotificationChannel.EMAIL
        );

        // Act
        publisher.publish(notification);

        // Assert
        ArgumentCaptor<NotificationCreatedMessage> messageCaptor =
                ArgumentCaptor.forClass(NotificationCreatedMessage.class);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.NOTIFICATION_EXCHANGE),
                eq(RabbitMQConfig.EMAIL_ROUTING_KEY),
                messageCaptor.capture()
        );

        NotificationCreatedMessage message = messageCaptor.getValue();

        assertThat(message.notificationId())
                .isEqualTo(notification.getId());

        assertThat(message.channel())
                .isEqualTo(NotificationChannel.EMAIL);

        assertThat(message.recipient())
                .isEqualTo(notification.getRecipient());

        assertThat(message.subject())
                .isEqualTo(notification.getSubject());

        assertThat(message.content())
                .isEqualTo(notification.getContent());

        assertThat(message.priority())
                .isEqualTo(notification.getPriority());
    }

    @Test
    void shouldPublishSmsNotificationUsingSmsRoutingKey() {

        // Arrange
        Notification notification = createNotification(
                NotificationChannel.SMS
        );

        // Act
        publisher.publish(notification);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.NOTIFICATION_EXCHANGE),
                eq(RabbitMQConfig.SMS_ROUTING_KEY),
                any(NotificationCreatedMessage.class)
        );
    }

    @Test
    void shouldPublishPushNotificationUsingPushRoutingKey() {

        // Arrange
        Notification notification = createNotification(
                NotificationChannel.PUSH
        );

        // Act
        publisher.publish(notification);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.NOTIFICATION_EXCHANGE),
                eq(RabbitMQConfig.PUSH_ROUTING_KEY),
                any(NotificationCreatedMessage.class)
        );
    }

    @Test
    void shouldFailWhenNoRoutingStrategySupportsChannel() {

        // Arrange
        List<NotificationRoutingStrategy> strategies = List.of();

        RabbitMQNotificationPublisher publisherWithoutStrategy =
                new RabbitMQNotificationPublisher(
                        rabbitTemplate,
                        strategies
                );

        Notification notification = createNotification(
                NotificationChannel.EMAIL
        );

        // Act + Assert
        assertThatThrownBy(
                () -> publisherWithoutStrategy.publish(notification)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Canal de notificação não suportado: EMAIL"
                );
    }

    private Notification createNotification(
            NotificationChannel channel
    ) {
        return new Notification(
                channel,
                "test@example.com",
                "Teste",
                "Conteúdo da notificação",
                NotificationPriority.NORMAL,
                UUID.fromString(
                        "550e8400-e29b-41d4-a716-446655440030"
                )
        );
    }
}
