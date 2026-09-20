package com.github.alfredobaptista.notification.adapter.out.persistence;

import com.github.alfredobaptista.notification.application.port.out.NotificationRepository;
import com.github.alfredobaptista.notification.domain.entity.Notification;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryAdapter
        implements NotificationRepository {

    private final SpringDataNotificationRepository springDataRepository;

    @Override
    public Notification save(Notification notification) {

        var entity = NotificationMapper.toEntity(notification);

        var savedEntity = springDataRepository.save(entity);

        return NotificationMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Notification> findById(UUID id) {

        return springDataRepository
                .findById(id)
                .map(NotificationMapper::toDomain);
    }

    @Override
    public Optional<Notification> findByIdempotencyKey(
            String idempotencyKey
    ) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        return springDataRepository
                .findByIdempotencyKey(idempotencyKey.trim())
                .map(NotificationMapper::toDomain);
    }
}