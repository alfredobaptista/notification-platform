package com.github.alfredobaptista.notification.application.service;

import com.github.alfredobaptista.notification.application.port.in.GetNotificationUseCase;
import com.github.alfredobaptista.notification.application.port.out.NotificationRepository;
import com.github.alfredobaptista.notification.domain.entity.Notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetNotificationService implements GetNotificationUseCase {

    private final NotificationRepository repository;

    @Override
    public Optional<Notification> execute(UUID id) {
        return repository.findById(id);
    }
}