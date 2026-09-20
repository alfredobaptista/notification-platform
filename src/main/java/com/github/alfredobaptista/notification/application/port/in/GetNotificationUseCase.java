package com.github.alfredobaptista.notification.application.port.in;

import com.github.alfredobaptista.notification.domain.entity.Notification;

import java.util.Optional;
import java.util.UUID;

public interface GetNotificationUseCase {

    Optional<Notification> execute(UUID id);
}