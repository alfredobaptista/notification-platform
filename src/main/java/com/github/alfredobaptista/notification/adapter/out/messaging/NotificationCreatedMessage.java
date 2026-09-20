package com.github.alfredobaptista.notification.adapter.out.messaging;

import com.github.alfredobaptista.notification.domain.entity.Notification;
import com.github.alfredobaptista.notification.domain.enums.NotificationChannel;
import com.github.alfredobaptista.notification.domain.enums.NotificationPriority;

import java.util.UUID;

public record NotificationCreatedMessage(
        UUID notificationId,
        NotificationChannel channel,
        String recipient,
        String subject,
        String content,
        NotificationPriority priority
) {

    public static NotificationCreatedMessage fromDomain(
            Notification notification
    ) {
        return new NotificationCreatedMessage(
                notification.getId(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getContent(),
                notification.getPriority()
        );
    }
}