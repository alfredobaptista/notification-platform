package com.github.alfredobaptista.notification.adapter.out.event.spring;

import com.github.alfredobaptista.notification.application.event.NotificationCreatedEvent;
import com.github.alfredobaptista.notification.application.port.out.NotificationPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCreatedEventListener {

    private final NotificationPublisher publisher;

    /**
     * Trata o evento de criação da notificação após o commit da transacção.
     *
     * O evento só é processado depois de a transacção da criação
     * da notificação ser confirmada com sucesso.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(NotificationCreatedEvent event) {

        var notification = event.notification();

        log.debug(
                "Transacção confirmada. A publicar notificação {} no RabbitMQ.",
                notification.getId()
        );

        publisher.publish(notification);

        log.info(
                "Notificação {} publicada no RabbitMQ após COMMIT.",
                notification.getId()
        );
    }
}