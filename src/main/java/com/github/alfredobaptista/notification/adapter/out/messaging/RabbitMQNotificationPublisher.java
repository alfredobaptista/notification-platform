package com.github.alfredobaptista.notification.adapter.out.messaging;

import com.github.alfredobaptista.notification.adapter.out.messaging.routing.NotificationRoutingStrategy;
import com.github.alfredobaptista.notification.application.port.out.NotificationPublisher;
import com.github.alfredobaptista.notification.domain.entity.Notification;
import com.github.alfredobaptista.notification.domain.enums.NotificationChannel;
import com.github.alfredobaptista.notification.infrastructure.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RabbitMQNotificationPublisher implements NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final List<NotificationRoutingStrategy> routingStrategies;

    @Override
    public void publish(Notification notification) {

        String routingKey = resolveRoutingKey(
                notification.getChannel()
        );

        var message = NotificationCreatedMessage.fromDomain(notification);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                routingKey,
                message
        );
    }

    private String resolveRoutingKey(NotificationChannel channel) {

        return routingStrategies.stream()
                .filter(strategy -> strategy.supports(channel))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Canal de notificação não suportado: "
                                        + channel
                        )
                )
                .getRoutingKey();
    }
}