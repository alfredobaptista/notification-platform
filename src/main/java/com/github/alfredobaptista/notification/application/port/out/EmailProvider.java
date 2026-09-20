package com.github.alfredobaptista.notification.application.port.out;

public interface EmailProvider {

    void send(
            String recipient,
            String subject,
            String content
    );
}