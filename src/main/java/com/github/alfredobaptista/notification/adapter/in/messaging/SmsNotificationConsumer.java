package com.github.alfredobaptista.notification.adapter.in.messaging;

import com.github.alfredobaptista.notification.adapter.out.messaging.NotificationCreatedMessage;
import com.github.alfredobaptista.notification.application.metrics.NotificationMetrics;
import com.github.alfredobaptista.notification.application.port.out.NotificationRepository;
import com.github.alfredobaptista.notification.domain.entity.Notification;
import com.github.alfredobaptista.notification.domain.enums.NotificationChannel;
import com.github.alfredobaptista.notification.infrastructure.config.RabbitMQConfig;

import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsNotificationConsumer {

    private static final int MAX_ATTEMPTS = 3;

    private final NotificationRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final NotificationMetrics metrics;

    @RabbitListener(
            queues = RabbitMQConfig.SMS_QUEUE,
            concurrency = "1-3"
    )
    public void consumeSms(
            NotificationCreatedMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {

        UUID notificationId = message.notificationId();

        log.info(
                "A processar notificação SMS ID: {}",
                notificationId
        );

        var notificationOpt = repository.findById(notificationId);

        if (notificationOpt.isEmpty()) {
            log.warn(
                    "Notificação {} não encontrada na base de dados. " +
                            "A descartar mensagem.",
                    notificationId
            );

            channel.basicAck(deliveryTag, false);
            return;
        }

        Notification notification = notificationOpt.get();

        try {

            /*
             * 1. Marca como PROCESSING.
             *
             * O domínio incrementa 'attempts'.
             */
            notification.markAsProcessing();
            repository.save(notification);

            metrics.notificationProcessing(
                    NotificationChannel.SMS
            );

            int currentAttempt = notification.getAttempts();

            log.debug(
                    "Notificação SMS {} marcada como PROCESSING. " +
                            "Tentativa {}/{}.",
                    notificationId,
                    currentAttempt,
                    MAX_ATTEMPTS
            );

            /*
             * 2. Comunicação com o provider SMS.
             */
            simulateSmsProviderCall(notification);

            /*
             * 3. Provider respondeu com sucesso.
             */
            notification.markAsDelivered();
            repository.save(notification);

            metrics.notificationDelivered(
                    NotificationChannel.SMS
            );

            /*
             * 4. ACK depois do processamento completo.
             */
            channel.basicAck(deliveryTag, false);

            log.info(
                    "Notificação SMS {} entregue com sucesso " +
                            "na tentativa {}/{}.",
                    notificationId,
                    currentAttempt,
                    MAX_ATTEMPTS
            );

        } catch (Exception exception) {

            log.error(
                    "Erro ao processar notificação SMS {} " +
                            "na tentativa {}/{} para {}: {}",
                    notificationId,
                    notification.getAttempts(),
                    MAX_ATTEMPTS,
                    notification.getRecipient(),
                    exception.getMessage(),
                    exception
            );

            handleFailure(
                    notification,
                    message,
                    channel,
                    deliveryTag,
                    exception
            );
        }
    }

    private void handleFailure(
            Notification notification,
            NotificationCreatedMessage message,
            Channel channel,
            long deliveryTag,
            Exception exception
    ) throws IOException {

        UUID notificationId = message.notificationId();
        int currentAttempt = notification.getAttempts();

        metrics.notificationFailed(
                NotificationChannel.SMS
        );

        /*
         * Ainda existem tentativas disponíveis.
         */
        if (currentAttempt < MAX_ATTEMPTS) {

            notification.markAsRetrying(
                    exception.getMessage()
            );

            repository.save(notification);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DLX_EXCHANGE,
                    RabbitMQConfig.SMS_RETRY_ROUTING_KEY,
                    message
            );

            metrics.notificationRetry(
                    NotificationChannel.SMS
            );

            /*
             * A mensagem original já foi encaminhada
             * para a fila de retry.
             */
            channel.basicAck(deliveryTag, false);

            log.warn(
                    "Notificação SMS {} falhou na tentativa {}/{}. " +
                            "A enviar para retry.",
                    notificationId,
                    currentAttempt,
                    MAX_ATTEMPTS
            );

        } else {

            /*
             * Todas as tentativas foram utilizadas.
             */
            notification.markAsDeadLetter(
                    "Esgotadas as tentativas. " +
                            "Erro final: " +
                            exception.getMessage()
            );

            repository.save(notification);

            /*
             * Rejeita sem requeue.
             *
             * O DLX da sms.queue encaminha a mensagem
             * para sms.dlq.
             */
            channel.basicReject(
                    deliveryTag,
                    false
            );

            metrics.notificationDeadLettered(
                    NotificationChannel.SMS
            );

            log.error(
                    "Notificação SMS {} movida para a DLQ " +
                            "após {} tentativas.",
                    notificationId,
                    MAX_ATTEMPTS
            );
        }
    }

    private void simulateSmsProviderCall(
            Notification notification
    ) {

        try {
            Thread.sleep(3000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Processamento SMS interrompido.",
                    exception
            );
        }

        if (notification.getRecipient().contains("fail")) {

            throw new IllegalStateException(
                    "Timeout de ligação com o fornecedor SMS " +
                            "(503 Service Unavailable)"
            );
        }
    }
}
