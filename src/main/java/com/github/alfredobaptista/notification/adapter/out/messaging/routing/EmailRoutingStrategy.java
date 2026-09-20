package com.github.alfredobaptista.notification.adapter.out.messaging.routing;

import com.github.alfredobaptista.notification.domain.enums.NotificationChannel;
import com.github.alfredobaptista.notification.infrastructure.config.RabbitMQConfig;

import org.springframework.stereotype.Component;

@Component
public class EmailRoutingStrategy implements NotificationRoutingStrategy {

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public String getRoutingKey() {
        return RabbitMQConfig.EMAIL_ROUTING_KEY;
    }
}