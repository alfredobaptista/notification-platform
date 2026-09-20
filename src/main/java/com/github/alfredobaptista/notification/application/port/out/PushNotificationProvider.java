package com.github.alfredobaptista.notification.application.port.out;

public interface PushNotificationProvider {

    void send(
            String deviceToken,
            String title,
            String content
    );
}
