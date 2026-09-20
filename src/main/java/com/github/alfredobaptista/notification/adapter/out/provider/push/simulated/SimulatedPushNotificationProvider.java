package com.github.alfredobaptista.notification.adapter.out.provider.push.simulated;

import com.github.alfredobaptista.notification.application.port.out.PushNotificationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SimulatedPushNotificationProvider implements PushNotificationProvider {

    @Override
    public void send(String deviceToken, String title, String content) {

        log.info(
                "PUSH simulado enviado com sucesso | deviceToken={} | título='{}' | conteúdo='{}'",
                maskToken(deviceToken),
                title,
                content
        );
    }

    private String maskToken(String token) {

        if (token == null || token.length() <= 8) {
            return "***";
        }

        return token.substring(0, 4)
                + "..."
                + token.substring(token.length() - 4);
    }
}