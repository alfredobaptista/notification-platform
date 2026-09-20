package com.github.alfredobaptista.notification.application.port.out;

import com.github.alfredobaptista.notification.application.event.NotificationCreatedEvent;

public interface NotificationEventPublisher {

    void publish(NotificationCreatedEvent event);
}