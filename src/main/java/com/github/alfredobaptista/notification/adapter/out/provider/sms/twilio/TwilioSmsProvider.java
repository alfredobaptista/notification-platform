package com.github.alfredobaptista.notification.adapter.out.provider.sms.twilio;

import com.github.alfredobaptista.notification.application.port.out.SmsProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class TwilioSmsProvider implements SmsProvider {

    private final TwilioProperties properties;

    private final RestClient twilioRestClient;

    @Override
    public void send(
            String recipient,
            String content
    ) {

        String accountSid = properties.accountSid();

        String credentials =
                accountSid + ":" + properties.authToken();

        String authorization =
                Base64.getEncoder()
                        .encodeToString(
                                credentials.getBytes(StandardCharsets.UTF_8)
                        );

        MultiValueMap<String, String> formData =
                new LinkedMultiValueMap<>();

        formData.add("To", recipient);
        formData.add("From", properties.fromNumber());
        formData.add("Body", content);

        log.debug(
                "A enviar SMS através da Twilio para {}.",
                recipient
        );

        twilioRestClient
                .post()
                .uri(
                        "/Accounts/{accountSid}/Messages.json",
                        accountSid
                )
                .header(
                        "Authorization",
                        "Basic " + authorization
                )
                .contentType(
                        MediaType.APPLICATION_FORM_URLENCODED
                )
                .body(formData)
                .retrieve()
                .toBodilessEntity();

        log.info(
                "SMS enviado com sucesso através da Twilio para {}.",
                recipient
        );
    }
}

