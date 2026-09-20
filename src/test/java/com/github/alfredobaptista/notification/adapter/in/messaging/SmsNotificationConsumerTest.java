package com.github.alfredobaptista.notification.adapter.in.messaging;

import com.github.alfredobaptista.notification.adapter.out.messaging.NotificationCreatedMessage;
import com.github.alfredobaptista.notification.application.metrics.NotificationMetrics;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsNotificationConsumerTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private NotificationMetrics metrics;

    @Mock
    private Channel channel;

    private SmsNotificationConsumer consumer;

    private Notification notification;

    private NotificationCreatedMessage message;

    private final long deliveryTag = 123L;

    @BeforeEach
    void setUp() {

        consumer = new SmsNotificationConsumer(
                repository,
                rabbitTemplate,
                metrics
        );

        notification = new Notification(
                NotificationChannel.SMS,
                "+244923456789",
                "Teste",
                "Conteúdo da notificação SMS",
                NotificationPriority.NORMAL,
                "idem-sms-001"
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
        consumer.consumeSms(
                message,
                channel,
                deliveryTag
        );

        // Assert
        verify(repository)
                .findById(notification.getId());

        verify(channel)
                .basicAck(deliveryTag, false);

        verifyNoInteractions(rabbitTemplate);
        verifyNoInteractions(metrics);
    }

    @Test
    void shouldProcessAndDeliverSmsSuccessfully()
            throws IOException {

        // Arrange
        when(repository.findById(notification.getId()))
                .thenReturn(Optional.of(notification));

        // Act
        consumer.consumeSms(
                message,
                channel,
                deliveryTag
        );

        // Assert
        assertThat(notification.getStatus())
                .isEqualTo(NotificationStatus.DELIVERED);

        assertThat(notification.getAttempts())
                .isEqualTo(1);

        verify(repository, times(2))
                .save(notification);

        verify(metrics)
                .notificationProcessing(NotificationChannel.SMS);

        verify(metrics)
                .notificationDelivered(NotificationChannel.SMS);

        verify(channel)
                .basicAck(deliveryTag, false);

        verify(channel, never())
                .basicReject(anyLong(), anyBoolean());

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void shouldScheduleRetryWhenSmsProcessingFailsOnFirstAttempt()
            throws IOException {

        // Arrange
        notification = new Notification(
                NotificationChannel.SMS,
                "fail@example.com",
                "Teste",
                "Conteúdo da notificação SMS",
                NotificationPriority.NORMAL,
                "idem-sms-fail-001"
        );

        message = NotificationCreatedMessage.fromDomain(notification);

        when(repository.findById(notification.getId()))
                .thenReturn(Optional.of(notification));

        // Act
        consumer.consumeSms(
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
                .contains("Timeout de ligação com o fornecedor SMS")
                .contains("503 Service Unavailable");

        verify(repository, times(2))
                .save(notification);

        verify(metrics)
                .notificationProcessing(NotificationChannel.SMS);

        verify(metrics)
                .notificationFailed(NotificationChannel.SMS);

        verify(metrics)
                .notificationRetry(NotificationChannel.SMS);

        verify(rabbitTemplate)
                .convertAndSend(
                        RabbitMQConfig.DLX_EXCHANGE,
                        RabbitMQConfig.SMS_RETRY_ROUTING_KEY,
                        message
                );

        verify(channel)
                .basicAck(deliveryTag, false);

        verify(channel, never())
                .basicReject(anyLong(), anyBoolean());
    }

    @Test
    void shouldScheduleSecondRetryWhenSmsProcessingFailsOnSecondAttempt()
            throws IOException {

        // Arrange
        notification = new Notification(
                NotificationChannel.SMS,
                "fail@example.com",
                "Teste",
                "Conteúdo da notificação SMS",
                NotificationPriority.NORMAL,
                "idem-sms-retry-002"
        );

        message = NotificationCreatedMessage.fromDomain(notification);

        when(repository.findById(notification.getId()))
                .thenReturn(Optional.of(notification));

        /*
         * Simula o estado após a primeira tentativa:
         *
         * Tentativa 1:
         * PROCESSING -> RETRYING
         *
         * Estado actual:
         * attempts = 1
         */
        notification.markAsProcessing();
        notification.markAsRetrying("Primeira falha");

        // Act
        consumer.consumeSms(
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
                .contains("Timeout de ligação com o fornecedor SMS")
                .contains("503 Service Unavailable");

        verify(repository, times(2))
                .save(notification);

        verify(metrics)
                .notificationProcessing(NotificationChannel.SMS);

        verify(metrics)
                .notificationFailed(NotificationChannel.SMS);

        verify(metrics)
                .notificationRetry(NotificationChannel.SMS);

        verify(rabbitTemplate)
                .convertAndSend(
                        RabbitMQConfig.DLX_EXCHANGE,
                        RabbitMQConfig.SMS_RETRY_ROUTING_KEY,
                        message
                );

        verify(channel)
                .basicAck(deliveryTag, false);

        verify(channel, never())
                .basicReject(anyLong(), anyBoolean());
    }

    @Test
    void shouldDeadLetterWhenSmsProcessingFailsOnThirdAttempt()
            throws IOException {

        // Arrange
        notification = new Notification(
                NotificationChannel.SMS,
                "fail@example.com",
                "Teste",
                "Conteúdo da notificação SMS",
                NotificationPriority.NORMAL,
                "idem-sms-dlq-003"
        );

        message = NotificationCreatedMessage.fromDomain(notification);

        when(repository.findById(notification.getId()))
                .thenReturn(Optional.of(notification));

        /*
         * Simula as duas tentativas anteriores:
         *
         * Tentativa 1:
         * PROCESSING -> RETRYING
         *
         * Tentativa 2:
         * PROCESSING -> RETRYING
         *
         * Estado actual:
         * attempts = 2
         */
        notification.markAsProcessing();
        notification.markAsRetrying("Primeira falha");

        notification.markAsProcessing();
        notification.markAsRetrying("Segunda falha");

        // Act
        consumer.consumeSms(
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
                .contains("Timeout de ligação com o fornecedor SMS")
                .contains("503 Service Unavailable");

        /*
         * O consumer salva:
         *
         * 1. PROCESSING
         * 2. DEAD_LETTER
         */
        verify(repository, times(2))
                .save(notification);

        verify(metrics)
                .notificationProcessing(NotificationChannel.SMS);

        verify(metrics)
                .notificationFailed(NotificationChannel.SMS);

        verify(metrics)
                .notificationDeadLettered(NotificationChannel.SMS);

        /*
         * Na terceira tentativa não deve
         * existir uma nova publicação
         * na retry queue.
         */
        verify(rabbitTemplate, never())
                .convertAndSend(
                        anyString(),
                        anyString(),
                        any(NotificationCreatedMessage.class)
                );

        /*
         * A mensagem é rejeitada sem requeue.
         *
         * O RabbitMQ encaminha a mensagem
         * para a DLQ através do DLX configurado.
         */
        verify(channel)
                .basicReject(deliveryTag, false);

        verify(channel, never())
                .basicAck(anyLong(), anyBoolean());
    }
}