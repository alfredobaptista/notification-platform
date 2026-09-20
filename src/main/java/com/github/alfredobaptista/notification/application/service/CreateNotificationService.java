package com.github.alfredobaptista.notification.application.service;

import com.github.alfredobaptista.notification.application.event.NotificationCreatedEvent;
import com.github.alfredobaptista.notification.application.metrics.NotificationMetrics;
import com.github.alfredobaptista.notification.application.port.in.CreateNotificationUseCase;
import com.github.alfredobaptista.notification.application.port.out.NotificationEventPublisher;
import com.github.alfredobaptista.notification.application.port.out.NotificationRepository;
import com.github.alfredobaptista.notification.domain.entity.Notification;
import com.github.alfredobaptista.notification.domain.enums.NotificationChannel;
import com.github.alfredobaptista.notification.domain.enums.NotificationPriority;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateNotificationService
        implements CreateNotificationUseCase {

    private final NotificationRepository repository;
    private final NotificationEventPublisher eventPublisher;
    private final NotificationMetrics metrics;

    @Override
    @Transactional
    public Notification execute(
            NotificationChannel channel,
            String recipient,
            String subject,
            String content,
            NotificationPriority priority,
            String idempotencyKey
    ) {

        var existing = repository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            return existing.get();
        }

        var notification = new Notification(
                channel,
                recipient,
                subject,
                content,
                priority,
                idempotencyKey
        );

        var saved = repository.save(notification);

        metrics.notificationCreated(
                saved.getChannel()
        );

        eventPublisher.publish(
                new NotificationCreatedEvent(saved)
        );

        return saved;
    }
}