package com.github.alfredobaptista.notification.domain.entity;

import com.github.alfredobaptista.notification.domain.enums.NotificationChannel;
import com.github.alfredobaptista.notification.domain.enums.NotificationPriority;
import com.github.alfredobaptista.notification.domain.enums.NotificationStatus;
import com.github.alfredobaptista.notification.domain.exception.InvalidNotificationException;
import com.github.alfredobaptista.notification.domain.exception.InvalidNotificationStateException;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

    @Test
    void shouldCreateNotificationWithDefaultValues() {

        // Arrange
        var before = OffsetDateTime.now();

        // Act
        var notification = new Notification(
                NotificationChannel.EMAIL,
                "user@example.com",
                "Test notification",
                "Hello World",
                null,
                "  notification-123  "
        );

        var after = OffsetDateTime.now();

        // Assert
        assertThat(notification.getId()).isNotNull();
        assertThat(notification.getChannel())
                .isEqualTo(NotificationChannel.EMAIL);
        assertThat(notification.getRecipient())
                .isEqualTo("user@example.com");
        assertThat(notification.getSubject())
                .isEqualTo("Test notification");
        assertThat(notification.getContent())
                .isEqualTo("Hello World");
        assertThat(notification.getStatus())
                .isEqualTo(NotificationStatus.PENDING);
        assertThat(notification.getPriority())
                .isEqualTo(NotificationPriority.NORMAL);
        assertThat(notification.getAttempts())
                .isZero();
        assertThat(notification.getIdempotencyKey())
                .isEqualTo("notification-123");
        assertThat(notification.getCreatedAt())
                .isBetween(before, after);
        assertThat(notification.getUpdatedAt())
                .isEqualTo(notification.getCreatedAt());
        assertThat(notification.getProcessedAt())
                .isNull();
        assertThat(notification.getLastError())
                .isNull();
    }

    @Test
    void shouldMarkNotificationAsProcessing() {

        // Arrange
        var notification = createNotification();

        // Act
        notification.markAsProcessing();

        // Assert
        assertThat(notification.getStatus())
                .isEqualTo(NotificationStatus.PROCESSING);
        assertThat(notification.getAttempts())
                .isEqualTo(1);
        assertThat(notification.getUpdatedAt())
                .isAfterOrEqualTo(notification.getCreatedAt());
    }

    @Test
    void shouldProcessNotificationAgainAfterRetry() {

        // Arrange
        var notification = createNotification();

        notification.markAsProcessing();
        notification.markAsRetrying("Temporary provider failure");

        // Act
        notification.markAsProcessing();

        // Assert
        assertThat(notification.getStatus())
                .isEqualTo(NotificationStatus.PROCESSING);
        assertThat(notification.getAttempts())
                .isEqualTo(2);
    }

    @Test
    void shouldMarkNotificationAsDelivered() {

        // Arrange
        var notification = createNotification();
        notification.markAsProcessing();
        notification.markAsRetrying("Temporary failure");

        // Act
        notification.markAsProcessing();
        notification.markAsDelivered();

        // Assert
        assertThat(notification.getStatus())
                .isEqualTo(NotificationStatus.DELIVERED);
        assertThat(notification.getProcessedAt())
                .isNotNull();
        assertThat(notification.getLastError())
                .isNull();
    }

    @Test
    void shouldMarkNotificationForRetry() {

        // Arrange
        var notification = createNotification();
        notification.markAsProcessing();

        // Act
        notification.markAsRetrying("  Provider unavailable  ");

        // Assert
        assertThat(notification.getStatus())
                .isEqualTo(NotificationStatus.RETRYING);
        assertThat(notification.getLastError())
                .isEqualTo("Provider unavailable");
    }

    @Test
    void shouldUseDefaultErrorWhenRetryErrorIsBlank() {

        // Arrange
        var notification = createNotification();
        notification.markAsProcessing();

        // Act
        notification.markAsRetrying("   ");

        // Assert
        assertThat(notification.getLastError())
                .isEqualTo("Unknown notification processing error");
    }

    @Test
    void shouldMarkNotificationAsDeadLetterFromProcessing() {

        // Arrange
        var notification = createNotification();
        notification.markAsProcessing();

        // Act
        notification.markAsDeadLetter("Permanent provider failure");

        // Assert
        assertThat(notification.getStatus())
                .isEqualTo(NotificationStatus.DEAD_LETTER);
        assertThat(notification.getLastError())
                .isEqualTo("Permanent provider failure");
    }

    @Test
    void shouldMarkNotificationAsDeadLetterFromRetrying() {

        // Arrange
        var notification = createNotification();
        notification.markAsProcessing();
        notification.markAsRetrying("Temporary failure");

        // Act
        notification.markAsDeadLetter("Maximum attempts reached");

        // Assert
        assertThat(notification.getStatus())
                .isEqualTo(NotificationStatus.DEAD_LETTER);
        assertThat(notification.getLastError())
                .isEqualTo("Maximum attempts reached");
    }

    @Test
    void shouldRejectNotificationWithNullChannel() {

        // Arrange
        var channel = (NotificationChannel) null;

        // Act & Assert
        assertThatThrownBy(() -> new Notification(
                channel,
                "user@example.com",
                "Subject",
                "Content",
                NotificationPriority.NORMAL,
                "key-123"
        ))
                .isInstanceOf(InvalidNotificationException.class)
                .hasMessage("Notification channel is required");
    }

    @Test
    void shouldRejectNotificationWithBlankRecipient() {

        // Arrange
        var recipient = "   ";

        // Act & Assert
        assertThatThrownBy(() -> new Notification(
                NotificationChannel.EMAIL,
                recipient,
                "Subject",
                "Content",
                NotificationPriority.NORMAL,
                "key-123"
        ))
                .isInstanceOf(InvalidNotificationException.class)
                .hasMessage("Notification recipient is required");
    }

    @Test
    void shouldRejectNotificationWithBlankContent() {

        // Arrange
        var content = "   ";

        // Act & Assert
        assertThatThrownBy(() -> new Notification(
                NotificationChannel.EMAIL,
                "user@example.com",
                "Subject",
                content,
                NotificationPriority.NORMAL,
                "key-123"
        ))
                .isInstanceOf(InvalidNotificationException.class)
                .hasMessage("Notification content is required");
    }

    @Test
    void shouldNotProcessDeliveredNotification() {

        // Arrange
        var notification = createNotification();
        notification.markAsProcessing();
        notification.markAsDelivered();

        // Act & Assert
        assertThatThrownBy(notification::markAsProcessing)
                .isInstanceOf(InvalidNotificationStateException.class);
    }

    @Test
    void shouldNotDeliverPendingNotification() {

        // Arrange
        var notification = createNotification();

        // Act & Assert
        assertThatThrownBy(notification::markAsDelivered)
                .isInstanceOf(InvalidNotificationStateException.class);
    }

    @Test
    void shouldNotRetryPendingNotification() {

        // Arrange
        var notification = createNotification();

        // Act & Assert
        assertThatThrownBy(() ->
                notification.markAsRetrying("Provider failure")
        )
                .isInstanceOf(InvalidNotificationStateException.class);
    }

    @Test
    void shouldNotMovePendingNotificationToDeadLetter() {

        // Arrange
        var notification = createNotification();

        // Act & Assert
        assertThatThrownBy(() ->
                notification.markAsDeadLetter("Permanent failure")
        )
                .isInstanceOf(InvalidNotificationStateException.class);
    }

    private Notification createNotification() {
        return new Notification(
                NotificationChannel.EMAIL,
                "user@example.com",
                "Test notification",
                "Hello World",
                NotificationPriority.NORMAL,
                "notification-test-key"
        );
    }
}