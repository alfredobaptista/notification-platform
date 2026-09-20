package com.github.alfredobaptista.notification.adapter.in.messaging;

import com.github.alfredobaptista.notification.adapter.out.messaging.NotificationCreatedMessage;
import com.github.alfredobaptista.notification.application.metrics.NotificationMetrics;
import com.github.alfredobaptista.notification.application.port.out.EmailProvider;
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
public class EmailNotificationConsumer {

    private static final int MAX_ATTEMPTS = 3;

    private final NotificationRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final EmailProvider emailProvider;
    private final NotificationMetrics metrics;

    @RabbitListener(
            queues = RabbitMQConfig.EMAIL_QUEUE,
            concurrency = "2-4"
    )
    public void consumeEmail(
            NotificationCreatedMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {

        UUID notificationId = message.notificationId();

        log.info(
                "A processar notificação de E-mail ID: {}",
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
             * O domínio incrementa 'attempts' neste momento.
             */
            notification.markAsProcessing();
            repository.save(notification);

            metrics.notificationProcessing(
                    NotificationChannel.EMAIL
            );

            int currentAttempt = notification.getAttempts();

            log.debug(
                    "Notificação de E-mail {} marcada como PROCESSING. " +
                            "Tentativa {}/{}.",
                    notificationId,
                    currentAttempt,
                    MAX_ATTEMPTS
            );

            /*
             * 2. Comunicação com o provider externo de E-mail.
             *
             * O consumer não conhece Brevo, Resend ou qualquer
             * outro fornecedor. Conhece apenas o contrato EmailProvider.
             */
            emailProvider.send(
                    notification.getRecipient(),
                    notification.getSubject(),
                    notification.getContent()
            );

            /*
             * 3. Provider respondeu com sucesso.
             */
            notification.markAsDelivered();
            repository.save(notification);

            metrics.notificationDelivered(
                    NotificationChannel.EMAIL
            );

            /*
             * 4. ACK apenas depois do processamento completo.
             */
            channel.basicAck(deliveryTag, false);

            log.info(
                    "Notificação de E-mail {} entregue com sucesso " +
                            "na tentativa {}/{}.",
                    notificationId,
                    currentAttempt,
                    MAX_ATTEMPTS
            );

        } catch (Exception exception) {

            log.error(
                    "Erro ao processar notificação de E-mail {} " +
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
                NotificationChannel.EMAIL
        );

        /*
         * Ainda existem tentativas disponíveis.
         *
         * tentativa 1 -> retry
         * tentativa 2 -> retry
         * tentativa 3 -> DLQ
         */
        if (currentAttempt < MAX_ATTEMPTS) {

            notification.markAsRetrying(
                    exception.getMessage()
            );

            repository.save(notification);

            /*
             * Envia a mensagem para a retry queue.
             *
             * A retry queue possui TTL de 5 segundos e,
             * depois do TTL, encaminha novamente para email.queue.
             */
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DLX_EXCHANGE,
                    RabbitMQConfig.EMAIL_RETRY_ROUTING_KEY,
                    message
            );

            metrics.notificationRetry(
                    NotificationChannel.EMAIL
            );

            /*
             * A mensagem original já foi encaminhada para retry.
             * Não fazemos requeue da mensagem original.
             */
            channel.basicAck(deliveryTag, false);

            log.warn(
                    "Notificação de E-mail {} falhou na tentativa {}/{}. " +
                            "A enviar para retry.",
                    notificationId,
                    currentAttempt,
                    MAX_ATTEMPTS
            );

        } else {

            /*
             * Limite de tentativas atingido.
             */
            notification.markAsDeadLetter(
                    "Esgotadas as tentativas. " +
                            "Erro final: " +
                            exception.getMessage()
            );

            repository.save(notification);

            /*
             * A email.queue possui DLX configurado.
             *
             * O RabbitMQ encaminhará a mensagem para email.dlq.
             */
            channel.basicReject(
                    deliveryTag,
                    false
            );

            metrics.notificationDeadLettered(
                    NotificationChannel.EMAIL
            );

            log.error(
                    "Notificação de E-mail {} movida para a DLQ " +
                            "após {} tentativas.",
                    notificationId,
                    MAX_ATTEMPTS
            );
        }
    }
}
