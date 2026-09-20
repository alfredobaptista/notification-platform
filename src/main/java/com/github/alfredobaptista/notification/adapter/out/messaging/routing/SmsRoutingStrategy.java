package com.github.alfredobaptista.notification.adapter.out.messaging.routing;

import com.github.alfredobaptista.notification.domain.enums.NotificationChannel;
import com.github.alfredobaptista.notification.infrastructure.config.RabbitMQConfig;

import org.springframework.stereotype.Component;

@Component
public class SmsRoutingStrategy implements NotificationRoutingStrategy {

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.SMS;
    }

    @Override
    public String getRoutingKey() {
        return RabbitMQConfig.SMS_ROUTING_KEY;
    }
}