package com.github.alfredobaptista.notification.adapter.out.provider.sms.twilio;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.providers.twilio")
public record TwilioProperties(

        String accountSid,

        String authToken,

        String fromNumber

) {
}
