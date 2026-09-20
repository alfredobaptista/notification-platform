package com.github.alfredobaptista.notification.adapter.out.event.spring;

import com.github.alfredobaptista.notification.application.event.NotificationCreatedEvent;
import com.github.alfredobaptista.notification.application.port.out.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringNotificationEventPublisher
        implements NotificationEventPublisher {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(NotificationCreatedEvent event) {
        publisher.publishEvent(event);
    }
}