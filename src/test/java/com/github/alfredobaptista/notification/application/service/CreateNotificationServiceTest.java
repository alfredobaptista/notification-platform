package com.github.alfredobaptista.notification.application.service;

import com.github.alfredobaptista.notification.application.event.NotificationCreatedEvent;
import com.github.alfredobaptista.notification.application.metrics.NotificationMetrics;
import com.github.alfredobaptista.notification.application.port.out.NotificationEventPublisher;
import com.github.alfredobaptista.notification.application.port.out.NotificationRepository;
import com.github.alfredobaptista.notification.domain.entity.Notification;
import com.github.alfredobaptista.notification.domain.enums.NotificationChannel;
import com.github.alfredobaptista.notification.domain.enums.NotificationPriority;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateNotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private NotificationEventPublisher eventPublisher;

    @Mock
    private NotificationMetrics metrics;

    private CreateNotificationService service;

    @BeforeEach
    void setUp() {
        service = new CreateNotificationService(
                repository,
                eventPublisher,
                metrics
        );
    }

    @Test
    void shouldCreateAndPublishNotification() {

        // Arrange
        var channel = NotificationChannel.EMAIL;
        var recipient = "user@example.com";
        var subject = "Test notification";
        var content = "Hello World";
        var priority = NotificationPriority.HIGH;

        var idempotencyKey = UUID.fromString(
                "550e8400-e29b-41d4-a716-446655440010"
        );

        when(repository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());

        when(repository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = service.execute(
                channel,
                recipient,
                subject,
                content,
                priority,
                idempotencyKey
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getChannel()).isEqualTo(channel);
        assertThat(result.getRecipient()).isEqualTo(recipient);
        assertThat(result.getSubject()).isEqualTo(subject);
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getPriority()).isEqualTo(priority);
        assertThat(result.getIdempotencyKey())
                .isEqualTo(idempotencyKey);

        verify(repository)
                .findByIdempotencyKey(idempotencyKey);

        verify(repository)
                .save(any(Notification.class));

        verify(metrics)
                .notificationCreated(channel);

        verify(eventPublisher)
                .publish(any(NotificationCreatedEvent.class));
    }

    @Test
    void shouldReturnExistingNotificationWhenIdempotencyKeyAlreadyExists() {

        // Arrange
        var idempotencyKey = UUID.fromString(
                "550e8400-e29b-41d4-a716-446655440011"
        );

        var existingNotification = new Notification(
                NotificationChannel.EMAIL,
                "user@example.com",
                "Existing notification",
                "Already created",
                NotificationPriority.NORMAL,
                idempotencyKey
        );

        when(repository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingNotification));

        // Act
        var result = service.execute(
                NotificationChannel.EMAIL,
                "another@example.com",
                "Another notification",
                "This should not be created",
                NotificationPriority.HIGH,
                idempotencyKey
        );

        // Assert
        assertThat(result)
                .isSameAs(existingNotification);

        verify(repository)
                .findByIdempotencyKey(idempotencyKey);

        verify(repository, never())
                .save(any(Notification.class));

        verify(metrics, never())
                .notificationCreated(any());

        verify(eventPublisher, never())
                .publish(any(NotificationCreatedEvent.class));
    }

    @Test
    void shouldSaveNotificationWithCorrectData() {

        // Arrange
        var idempotencyKey = UUID.fromString(
                "550e8400-e29b-41d4-a716-446655440012"
        );

        when(repository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());

        when(repository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        service.execute(
                NotificationChannel.SMS,
                "+244900000000",
                "SMS test",
                "Mensagem de teste",
                NotificationPriority.HIGH,
                idempotencyKey
        );

        // Assert
        var captor = ArgumentCaptor.forClass(Notification.class);

        verify(repository).save(captor.capture());

        var savedNotification = captor.getValue();

        assertThat(savedNotification.getChannel())
                .isEqualTo(NotificationChannel.SMS);
        assertThat(savedNotification.getRecipient())
                .isEqualTo("+244900000000");
        assertThat(savedNotification.getSubject())
                .isEqualTo("SMS test");
        assertThat(savedNotification.getContent())
                .isEqualTo("Mensagem de teste");
        assertThat(savedNotification.getPriority())
                .isEqualTo(NotificationPriority.HIGH);
        assertThat(savedNotification.getIdempotencyKey())
                .isEqualTo(idempotencyKey);
    }

    @Test
    void shouldPublishEventWithSavedNotification() {

        // Arrange
        var idempotencyKey = UUID.fromString(
                "550e8400-e29b-41d4-a716-446655440013"
        );

        when(repository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());

        when(repository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = service.execute(
                NotificationChannel.PUSH,
                "device-token",
                "Push test",
                "Mensagem push",
                NotificationPriority.NORMAL,
                idempotencyKey
        );

        // Assert
        var captor = ArgumentCaptor.forClass(
                NotificationCreatedEvent.class
        );

        verify(eventPublisher)
                .publish(captor.capture());

        var publishedEvent = captor.getValue();

        assertThat(publishedEvent.notification())
                .isSameAs(result);
    }
}
