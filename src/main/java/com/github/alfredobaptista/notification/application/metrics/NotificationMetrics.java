package com.github.alfredobaptista.notification.application.metrics;

import com.github.alfredobaptista.notification.domain.enums.NotificationChannel;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetrics {

    private final MeterRegistry meterRegistry;

    public NotificationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void notificationCreated(NotificationChannel channel) {
        counter("notification.created", channel).increment();
    }

    public void notificationProcessing(NotificationChannel channel) {
        counter("notification.processing", channel).increment();
    }

    public void notificationDelivered(NotificationChannel channel) {
        counter("notification.delivered", channel).increment();
    }

    public void notificationFailed(NotificationChannel channel) {
        counter("notification.failed", channel).increment();
    }

    public void notificationRetry(NotificationChannel channel) {
        counter("notification.retry", channel).increment();
    }

    public void notificationDeadLettered(NotificationChannel channel) {
        counter("notification.dead_lettered", channel).increment();
    }

    private Counter counter(String name, NotificationChannel channel) {
        return Counter.builder(name)
                .description("Métrica de notificações: " + name)
                .tag("channel", channel.name())
                .register(meterRegistry);
    }
}