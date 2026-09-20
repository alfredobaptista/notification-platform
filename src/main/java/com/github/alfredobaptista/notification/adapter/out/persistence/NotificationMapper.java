package com.github.alfredobaptista.notification.adapter.out.persistence;

import com.github.alfredobaptista.notification.domain.entity.Notification;

public final class NotificationMapper {

    private NotificationMapper() {
        // Utility class
    }

    public static Notification toDomain(NotificationEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Notification(
                entity.getId(),
                entity.getChannel(),
                entity.getRecipient(),
                entity.getSubject(),
                entity.getContent(),
                entity.getStatus(),
                entity.getPriority(),
                entity.getAttempts(),
                entity.getIdempotencyKey(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getProcessedAt(),
                entity.getLastError()
        );
    }

    public static NotificationEntity toEntity(Notification domain) {
        if (domain == null) {
            return null;
        }

        return NotificationEntity.builder()
                .id(domain.getId())
                .channel(domain.getChannel())
                .recipient(domain.getRecipient())
                .subject(domain.getSubject())
                .content(domain.getContent())
                .status(domain.getStatus())
                .priority(domain.getPriority())
                .attempts(domain.getAttempts())
                .idempotencyKey(domain.getIdempotencyKey())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .processedAt(domain.getProcessedAt())
                .lastError(domain.getLastError())
                .build();
    }
}