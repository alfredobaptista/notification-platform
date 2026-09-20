package com.github.alfredobaptista.notification.application.event;

import com.github.alfredobaptista.notification.domain.entity.Notification;

public record NotificationCreatedEvent(
        Notification notification
) {
}