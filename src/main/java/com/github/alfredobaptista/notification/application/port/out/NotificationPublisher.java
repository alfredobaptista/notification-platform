package com.github.alfredobaptista.notification.application.port.out;

import com.github.alfredobaptista.notification.domain.entity.Notification;

public interface NotificationPublisher {

    void publish(Notification notification);
}