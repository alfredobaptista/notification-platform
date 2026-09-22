package com.github.alfredobaptista.notification.adapter.out.provider.email.brevo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.providers.brevo")
public record BrevoProperties(
        String baseUrl,
        String apiKey,
        String senderEmail,
        String senderName

) {
}
