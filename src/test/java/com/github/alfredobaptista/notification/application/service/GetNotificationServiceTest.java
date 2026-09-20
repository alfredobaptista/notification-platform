package com.github.alfredobaptista.notification.application.service;

import com.github.alfredobaptista.notification.domain.entity.Notification;
import com.github.alfredobaptista.notification.domain.enums.NotificationChannel;
import com.github.alfredobaptista.notification.domain.enums.NotificationPriority;
import com.github.alfredobaptista.notification.application.port.out.NotificationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetNotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    private GetNotificationService service;

    @BeforeEach
    void setUp() {
        service = new GetNotificationService(repository);
    }

    @Test
    void shouldReturnNotificationWhenItExists() {

        // Arrange
        var notificationId = UUID.randomUUID();

        var notification = new Notification(
                NotificationChannel.EMAIL,
                "user@example.com",
                "Test notification",
                "Hello World",
                NotificationPriority.NORMAL,
                "notification-123"
        );

        when(repository.findById(notificationId))
                .thenReturn(Optional.of(notification));

        // Act
        var result = service.execute(notificationId);

        // Assert
        assertThat(result)
                .isPresent()
                .containsSame(notification);

        verify(repository)
                .findById(notificationId);
    }

    @Test
    void shouldReturnEmptyWhenNotificationDoesNotExist() {

        // Arrange
        var notificationId = UUID.randomUUID();

        when(repository.findById(notificationId))
                .thenReturn(Optional.empty());

        // Act
        var result = service.execute(notificationId);

        // Assert
        assertThat(result)
                .isEmpty();

        verify(repository)
                .findById(notificationId);
    }
}