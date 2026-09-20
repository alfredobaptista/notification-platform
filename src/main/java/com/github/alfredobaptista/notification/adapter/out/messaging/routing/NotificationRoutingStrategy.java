package com.github.alfredobaptista.notification.adapter.out.messaging.routing;

import com.github.alfredobaptista.notification.domain.enums.NotificationChannel;

public interface NotificationRoutingStrategy {

    boolean supports(NotificationChannel channel);

    String getRoutingKey();
}