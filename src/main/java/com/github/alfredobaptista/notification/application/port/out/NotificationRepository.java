package com.github.alfredobaptista.notification.application.port.out;

import com.github.alfredobaptista.notification.domain.entity.Notification;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    Optional<Notification> findByIdempotencyKey(UUID idempotencyKey);
}