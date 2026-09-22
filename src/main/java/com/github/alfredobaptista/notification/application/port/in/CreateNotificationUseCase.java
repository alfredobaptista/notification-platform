package com.github.alfredobaptista.notification.application.port.in;

import java.util.UUID;

import com.github.alfredobaptista.notification.domain.entity.Notification;
import com.github.alfredobaptista.notification.domain.enums.NotificationChannel;
import com.github.alfredobaptista.notification.domain.enums.NotificationPriority;

public interface CreateNotificationUseCase {

    Notification execute(
            NotificationChannel channel,
            String recipient,
            String subject,
            String content,
            NotificationPriority priority,
            UUID idempotencyKey
    );
}