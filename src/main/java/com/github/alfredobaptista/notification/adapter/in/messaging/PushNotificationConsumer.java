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
public class PushNotificationConsumer {

    private static final int MAX_ATTEMPTS = 3;

    private final NotificationRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final NotificationMetrics metrics;

    @RabbitListener(
            queues = RabbitMQConfig.PUSH_QUEUE,
            concurrency = "1-3"
    )
    public void consumePush(
            NotificationCreatedMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {

        UUID notificationId = message.notificationId();

        log.info(
                "A processar notificação Push ID: {}",
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
                    NotificationChannel.PUSH
            );

            int currentAttempt = notification.getAttempts();

            log.debug(
                    "Notificação Push {} marcada como PROCESSING. " +
                            "Tentativa {}/{}.",
                    notificationId,
                    currentAttempt,
                    MAX_ATTEMPTS
            );

            /*
             * 2. Comunicação com o provider Push simulado.
             */
            simulatePushProviderCall(notification);

            /*
             * 3. Provider simulado respondeu com sucesso.
             */
            notification.markAsDelivered();
            repository.save(notification);

            metrics.notificationDelivered(
                    NotificationChannel.PUSH
            );

            /*
             * 4. ACK depois do processamento completo.
             */
            channel.basicAck(deliveryTag, false);

            log.info(
                    "Notificação Push {} entregue com sucesso " +
                            "na tentativa {}/{}.",
                    notificationId,
                    currentAttempt,
                    MAX_ATTEMPTS
            );

        } catch (Exception exception) {

            log.error(
                    "Erro ao processar notificação Push {} " +
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
                NotificationChannel.PUSH
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
                    RabbitMQConfig.PUSH_RETRY_ROUTING_KEY,
                    message
            );

            metrics.notificationRetry(
                    NotificationChannel.PUSH
            );

            /*
             * A mensagem original já foi encaminhada
             * para a fila de retry.
             */
            channel.basicAck(deliveryTag, false);

            log.warn(
                    "Notificação Push {} falhou na tentativa {}/{}. " +
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
             * O DLX da push.queue encaminha a mensagem
             * para push.dlq.
             */
            channel.basicReject(
                    deliveryTag,
                    false
            );

            metrics.notificationDeadLettered(
                    NotificationChannel.PUSH
            );

            log.error(
                    "Notificação Push {} movida para a DLQ " +
                            "após {} tentativas.",
                    notificationId,
                    MAX_ATTEMPTS
            );
        }
    }

    private void simulatePushProviderCall(
            Notification notification
    ) throws Exception {

        /*
         * Simulação de falha para testar Retry/DLQ.
         *
         * Exemplo:
         * recipient = "fail@example.com"
         */
        if (notification.getRecipient().contains("fail")) {

            throw new RuntimeException(
                    "Falha no provider Push simulado " +
                            "(Provider Unavailable)"
            );
        }
    }
}
