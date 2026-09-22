package com.github.alfredobaptista.notification.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataNotificationRepository
        extends JpaRepository<NotificationEntity, UUID> {

    Optional<NotificationEntity> findByIdempotencyKey(UUID idempotencyKey);
}