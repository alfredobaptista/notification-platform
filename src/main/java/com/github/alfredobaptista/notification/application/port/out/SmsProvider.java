
package com.github.alfredobaptista.notification.application.port.out;

public interface SmsProvider {

    void send(
            String recipient,
            String content
    );
}

