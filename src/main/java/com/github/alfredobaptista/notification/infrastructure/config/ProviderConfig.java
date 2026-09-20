package com.github.alfredobaptista.notification.infrastructure.config;

import com.github.alfredobaptista.notification.adapter.out.provider.email.brevo.BrevoProperties;
import com.github.alfredobaptista.notification.adapter.out.provider.sms.twilio.TwilioProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({
        BrevoProperties.class,
        TwilioProperties.class
})
public class ProviderConfig {

    @Bean
    public RestClient brevoRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .build();
    }

    @Bean
    public RestClient twilioRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.twilio.com/2010-04-01")
                .build();
    }
}

