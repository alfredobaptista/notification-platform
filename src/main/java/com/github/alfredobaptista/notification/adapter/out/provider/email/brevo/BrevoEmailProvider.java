package com.github.alfredobaptista.notification.adapter.out.provider.email.brevo;

import com.github.alfredobaptista.notification.application.port.out.EmailProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrevoEmailProvider implements EmailProvider {

private final BrevoProperties properties;
private final RestClient brevoRestClient;

@Override
public void send(
        String recipient,
        String subject,
        String content
) {

    var body = Map.of(
            "sender", Map.of(
                    "name", properties.senderName(),
                    "email", properties.senderEmail()
            ),
            "to", new Object[]{
                    Map.of(
                            "email", recipient
                    )
            },
            "subject", subject,
            "textContent", content
    );

    log.debug(
            "A enviar E-mail através da Brevo para {}.",
            recipient
    );

    brevoRestClient
            .post()
            .uri("/smtp/email")
            .header("api-key", properties.apiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toBodilessEntity();

    log.info(
            "E-mail enviado com sucesso através da Brevo para {}.",
            recipient
    );
}


}
