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
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PushNotificationConsumerTest {

    private static final UUID NOTIFICATION_ID =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440060");

    private static final UUID IDEMPOTENCY_KEY =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440061");

    private static final long DELIVERY_TAG = 123L;

    private NotificationRepository repository;
    private RabbitTemplate rabbitTemplate;
    private NotificationMetrics metrics;
    private Channel channel;

    private PushNotificationConsumer consumer;

    @BeforeEach
    void setUp() {

        repository = mock(NotificationRepository.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        metrics = mock(NotificationMetrics.class);
        channel = mock(Channel.class);

        consumer = new PushNotificationConsumer(
                repository,
                rabbitTemplate,
                metrics
        );
    }

    @Test
    void shouldDeliverPushNotificationSuccessfully()
            throws IOException {

        Notification notification =
                createNotification("device-token-123");

        NotificationCreatedMessage message =
                NotificationCreatedMessage.fromDomain(notification);

        when(repository.findById(NOTIFICATION_ID))
                .thenReturn(Optional.of(notification));

        consumer.consumePush(
                message,
                channel,
                DELIVERY_TAG
        );

        assertEquals(
                NotificationStatus.DELIVERED,
                notification.getStatus()
        );

        assertEquals(
                1,
                notification.getAttempts()
        );

        assertTrue(
                notification.getLastError() == null
        );

        verify(repository, atLeast(2))
                .save(notification);

        verify(metrics)
                .notificationProcessing(
                        NotificationChannel.PUSH
                );

        verify(metrics)
                .notificationDelivered(
                        NotificationChannel.PUSH
                );

        verify(channel)
                .basicAck(
                        DELIVERY_TAG,
                        false
                );

        verify(channel, never())
                .basicReject(
                        anyLong(),
                        anyBoolean()
                );

        verifyNoInteractions(
                rabbitTemplate
        );
    }

    @Test
    void shouldSendPushNotificationToRetryWhenProviderFails()
            throws IOException {

        Notification notification =
                createNotification("fail@example.com");

        NotificationCreatedMessage message =
                NotificationCreatedMessage.fromDomain(notification);

        when(repository.findById(NOTIFICATION_ID))
                .thenReturn(Optional.of(notification));

        consumer.consumePush(
                message,
                channel,
                DELIVERY_TAG
        );

        assertEquals(
                NotificationStatus.RETRYING,
                notification.getStatus()
        );

        assertEquals(
                1,
                notification.getAttempts()
        );

        assertTrue(
                notification.getLastError()
                        .contains(
                                "Falha no provider Push simulado"
                        )
        );

        verify(repository, atLeast(2))
                .save(notification);

        verify(metrics)
                .notificationProcessing(
                        NotificationChannel.PUSH
                );

        verify(metrics)
                .notificationFailed(
                        NotificationChannel.PUSH
                );

        verify(metrics)
                .notificationRetry(
                        NotificationChannel.PUSH
                );

        verify(rabbitTemplate)
                .convertAndSend(
                        eq(RabbitMQConfig.DLX_EXCHANGE),
                        eq(RabbitMQConfig.PUSH_RETRY_ROUTING_KEY),
                        eq(message)
                );

        verify(channel)
                .basicAck(
                        DELIVERY_TAG,
                        false
                );

        verify(channel, never())
                .basicReject(
                        anyLong(),
                        anyBoolean()
                );

        verify(metrics, never())
                .notificationDelivered(
                        NotificationChannel.PUSH
                );
    }

    @Test
    void shouldMovePushNotificationToDeadLetterAfterMaxAttempts()
            throws IOException {

        OffsetDateTime now =
                OffsetDateTime.now();

        /*
         * Representa uma notificação persistida que já
         * sofreu duas tentativas e está pronta para a terceira.
         *
         * O consumer executará:
         *
         * RETRYING + attempts=2
         *        ↓
         * markAsProcessing()
         *        ↓
         * PROCESSING + attempts=3
         *        ↓
         * provider falha
         *        ↓
         * DEAD_LETTER
         */
        Notification notification =
                new Notification(
                        NOTIFICATION_ID,
                        NotificationChannel.PUSH,
                        "fail@example.com",
                        "Push notification",
                        "Conteúdo da notificação Push",
                        NotificationStatus.RETRYING,
                        NotificationPriority.NORMAL,
                        2,
                        IDEMPOTENCY_KEY,
                        now,
                        now,
                        null,
                        "Falha anterior no provider Push"
                );

        NotificationCreatedMessage message =
                NotificationCreatedMessage.fromDomain(notification);

        when(repository.findById(NOTIFICATION_ID))
                .thenReturn(Optional.of(notification));

        consumer.consumePush(
                message,
                channel,
                DELIVERY_TAG
        );

        assertEquals(
                NotificationStatus.DEAD_LETTER,
                notification.getStatus()
        );

        assertEquals(
                3,
                notification.getAttempts()
        );

        assertTrue(
                notification.getLastError()
                        .contains(
                                "Esgotadas as tentativas"
                        )
        );

        verify(repository, atLeast(2))
                .save(notification);

        verify(metrics)
                .notificationProcessing(
                        NotificationChannel.PUSH
                );

        verify(metrics)
                .notificationFailed(
                        NotificationChannel.PUSH
                );

        verify(metrics)
                .notificationDeadLettered(
                        NotificationChannel.PUSH
                );

        verify(channel)
                .basicReject(
                        DELIVERY_TAG,
                        false
                );

        verify(channel, never())
                .basicAck(
                        anyLong(),
                        anyBoolean()
                );

        verify(rabbitTemplate, never())
                .convertAndSend(
                        any(String.class),
                        any(String.class),
                        any(Object.class)
                );

        verify(metrics, never())
                .notificationRetry(
                        NotificationChannel.PUSH
                );

        verify(metrics, never())
                .notificationDelivered(
                        NotificationChannel.PUSH
                );
    }

    @Test
    void shouldAckAndDiscardMessageWhenNotificationDoesNotExist()
            throws IOException {

        NotificationCreatedMessage message =
                new NotificationCreatedMessage(
                        NOTIFICATION_ID,
                        NotificationChannel.PUSH,
                        "device-token-123",
                        "Push notification",
                        "Conteúdo da notificação Push",
                        NotificationPriority.NORMAL
                );

        when(repository.findById(NOTIFICATION_ID))
                .thenReturn(Optional.empty());

        consumer.consumePush(
                message,
                channel,
                DELIVERY_TAG
        );

        verify(channel)
                .basicAck(
                        DELIVERY_TAG,
                        false
                );

        verify(repository, never())
                .save(any(Notification.class));

        verifyNoInteractions(
                rabbitTemplate,
                metrics
        );
    }

    private Notification createNotification(
            String recipient
    ) {

        OffsetDateTime now =
                OffsetDateTime.now();

        return new Notification(
                NOTIFICATION_ID,
                NotificationChannel.PUSH,
                recipient,
                "Push notification",
                "Conteúdo da notificação Push",
                NotificationStatus.PENDING,
                NotificationPriority.NORMAL,
                0,
                IDEMPOTENCY_KEY,
                now,
                now,
                null,
                null
        );
    }

}
