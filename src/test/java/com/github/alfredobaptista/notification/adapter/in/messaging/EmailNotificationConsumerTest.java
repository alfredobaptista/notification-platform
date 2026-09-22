package com.github.alfredobaptista.notification.adapter.in.messaging;

import com.github.alfredobaptista.notification.adapter.out.messaging.NotificationCreatedMessage;
import com.github.alfredobaptista.notification.application.metrics.NotificationMetrics;
import com.github.alfredobaptista.notification.application.port.out.EmailProvider;
import com.github.alfredobaptista.notification.application.port.out.NotificationRepository;
import com.github.alfredobaptista.notification.domain.entity.Notification;
import com.github.alfredobaptista.notification.domain.enums.NotificationChannel;
import com.github.alfredobaptista.notification.domain.enums.NotificationPriority;
import com.github.alfredobaptista.notification.domain.enums.NotificationStatus;
import com.github.alfredobaptista.notification.infrastructure.config.RabbitMQConfig;

import com.rabbitmq.client.Channel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotificationConsumerTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private EmailProvider emailProvider;

    @Mock
    private NotificationMetrics metrics;

    @Mock
    private Channel channel;

    private EmailNotificationConsumer consumer;

    private Notification notification;

    private NotificationCreatedMessage message;

    private final long deliveryTag = 123L;

    @BeforeEach
    void setUp() {

        consumer = new EmailNotificationConsumer(
                repository,
                rabbitTemplate,
                emailProvider,
                metrics
        );

        notification = new Notification(
                NotificationChannel.EMAIL,
                "cliente@example.com",
                "Teste",
                "Conteúdo da notificação",
                NotificationPriority.NORMAL,
                UUID.fromString(
                        "550e8400-e29b-41d4-a716-446655440040"
                )
        );

        message = NotificationCreatedMessage.fromDomain(notification);
    }

    @Test
    void shouldAckMessageWhenNotificationDoesNotExist()
            throws IOException {

        // Arrange
        when(repository.findById(notification.getId()))
                .thenReturn(Optional.empty());

        // Act
        consumer.consumeEmail(
                message,
                channel,
                deliveryTag
        );

        // Assert
        verify(repository).findById(notification.getId());

        verify(channel).basicAck(
                deliveryTag,
                false
        );

        verifyNoInteractions(emailProvider);
        verifyNoInteractions(rabbitTemplate);
        verifyNoInteractions(metrics);
    }

    @Test
    void shouldProcessAndDeliverEmailSuccessfully()
            throws IOException {

        // Arrange
        when(repository.findById(notification.getId()))
                .thenReturn(Optional.of(notification));

        // Act
        consumer.consumeEmail(
                message,
                channel,
                deliveryTag
        );

        // Assert
        verify(emailProvider).send(
                notification.getRecipient(),
                notification.getSubject(),
                notification.getContent()
        );

        verify(repository, times(2))
                .save(notification);

        verify(metrics)
                .notificationProcessing(NotificationChannel.EMAIL);

        verify(metrics)
                .notificationDelivered(NotificationChannel.EMAIL);

        verify(channel).basicAck(
                deliveryTag,
                false
        );

        verify(channel, never())
                .basicReject(anyLong(), anyBoolean());

        verifyNoInteractions(rabbitTemplate);

        assertThat(notification.getStatus())
                .isEqualTo(NotificationStatus.DELIVERED);

        assertThat(notification.getAttempts())
                .isEqualTo(1);
    }

    @Test
    void shouldScheduleRetryWhenEmailProcessingFailsOnFirstAttempt()
            throws IOException {

        // Arrange
        when(repository.findById(notification.getId()))
                .thenReturn(Optional.of(notification));

        RuntimeException exception =
                new RuntimeException("Falha temporária no provedor");

        doThrow(exception)
                .when(emailProvider)
                .send(
                        notification.getRecipient(),
                        notification.getSubject(),
                        notification.getContent()
                );

        // Act
        consumer.consumeEmail(
                message,
                channel,
                deliveryTag
        );

        // Assert
        assertThat(notification.getStatus())
                .isEqualTo(NotificationStatus.RETRYING);

        assertThat(notification.getAttempts())
                .isEqualTo(1);

        assertThat(notification.getLastError())
                .isEqualTo("Falha temporária no provedor");

        verify(metrics)
                .notificationProcessing(NotificationChannel.EMAIL);

        verify(metrics)
                .notificationFailed(NotificationChannel.EMAIL);

        verify(metrics)
                .notificationRetry(NotificationChannel.EMAIL);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.DLX_EXCHANGE,
                RabbitMQConfig.EMAIL_RETRY_ROUTING_KEY,
                message
        );

        verify(channel).basicAck(
                deliveryTag,
                false
        );

        verify(channel, never())
                .basicReject(anyLong(), anyBoolean());
    }

    @Test
    void shouldScheduleSecondRetryWhenEmailProcessingFailsOnSecondAttempt()
            throws IOException {

        // Arrange
        when(repository.findById(notification.getId()))
                .thenReturn(Optional.of(notification));

        notification.markAsProcessing();
        notification.markAsRetrying("Primeira falha");

        RuntimeException exception =
                new RuntimeException("Segunda falha temporária");

        doThrow(exception)
                .when(emailProvider)
                .send(
                        notification.getRecipient(),
                        notification.getSubject(),
                        notification.getContent()
                );

        // Act
        consumer.consumeEmail(
                message,
                channel,
                deliveryTag
        );

        // Assert
        assertThat(notification.getStatus())
                .isEqualTo(NotificationStatus.RETRYING);

        assertThat(notification.getAttempts())
                .isEqualTo(2);

        assertThat(notification.getLastError())
                .isEqualTo("Segunda falha temporária");

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.DLX_EXCHANGE,
                RabbitMQConfig.EMAIL_RETRY_ROUTING_KEY,
                message
        );

        verify(metrics)
                .notificationFailed(NotificationChannel.EMAIL);

        verify(metrics)
                .notificationRetry(NotificationChannel.EMAIL);

        verify(channel).basicAck(
                deliveryTag,
                false
        );

        verify(channel, never())
                .basicReject(anyLong(), anyBoolean());
    }

    @Test
    void shouldDeadLetterWhenEmailProcessingFailsOnThirdAttempt()
            throws IOException {

        // Arrange
        when(repository.findById(notification.getId()))
                .thenReturn(Optional.of(notification));

        notification.markAsProcessing();
        notification.markAsRetrying("Primeira falha");

        notification.markAsProcessing();
        notification.markAsRetrying("Segunda falha");

        RuntimeException exception =
                new RuntimeException("Falha definitiva");

        doThrow(exception)
                .when(emailProvider)
                .send(
                        notification.getRecipient(),
                        notification.getSubject(),
                        notification.getContent()
                );

        // Act
        consumer.consumeEmail(
                message,
                channel,
                deliveryTag
        );

        // Assert
        assertThat(notification.getStatus())
                .isEqualTo(NotificationStatus.DEAD_LETTER);

        assertThat(notification.getAttempts())
                .isEqualTo(3);

        assertThat(notification.getLastError())
                .contains("Esgotadas as tentativas")
                .contains("Falha definitiva");

        verify(metrics)
                .notificationFailed(NotificationChannel.EMAIL);

        verify(metrics)
                .notificationDeadLettered(NotificationChannel.EMAIL);

        verify(rabbitTemplate, never())
                .convertAndSend(
                        anyString(),
                        anyString(),
                        any(NotificationCreatedMessage.class)
                );

        verify(channel).basicReject(
                deliveryTag,
                false
        );

        verify(channel, never())
                .basicAck(anyLong(), anyBoolean());
    }
}