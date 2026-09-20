package com.github.alfredobaptista.notification.infrastructure.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_EXCHANGE =
            "notification.exchange";

    public static final String DLX_EXCHANGE =
            "notification.dlx";

    // =========================================================
    // EMAIL
    // =========================================================

    public static final String EMAIL_QUEUE =
            "email.queue";

    public static final String EMAIL_ROUTING_KEY =
            "notification.email";

    public static final String EMAIL_RETRY_QUEUE =
            "email.retry.queue";

    public static final String EMAIL_RETRY_ROUTING_KEY =
            "notification.email.retry";

    public static final String EMAIL_DLQ =
            "email.dlq";

    public static final String EMAIL_DLQ_ROUTING_KEY =
            "notification.email.dlq";

    // =========================================================
    // SMS
    // =========================================================

    public static final String SMS_QUEUE =
            "sms.queue";

    public static final String SMS_ROUTING_KEY =
            "notification.sms";

    public static final String SMS_RETRY_QUEUE =
            "sms.retry.queue";

    public static final String SMS_RETRY_ROUTING_KEY =
            "notification.sms.retry";

    public static final String SMS_DLQ =
            "sms.dlq";

    public static final String SMS_DLQ_ROUTING_KEY =
            "notification.sms.dlq";

    // =========================================================
    // PUSH
    // =========================================================

    public static final String PUSH_QUEUE =
            "push.queue";

    public static final String PUSH_ROUTING_KEY =
            "notification.push";

    public static final String PUSH_RETRY_QUEUE =
            "push.retry.queue";

    public static final String PUSH_RETRY_ROUTING_KEY =
            "notification.push.retry";

    public static final String PUSH_DLQ =
            "push.dlq";

    public static final String PUSH_DLQ_ROUTING_KEY =
            "notification.push.dlq";

    // =========================================================
    // EXCHANGES
    // =========================================================

    /**
     * Exchange principal responsável pelo encaminhamento
     * das notificações para as filas dos respectivos canais.
     */
    @Bean
    public DirectExchange notificationExchange() {

        return new DirectExchange(
                NOTIFICATION_EXCHANGE,
                true,
                false
        );
    }

    /**
     * Dead Letter Exchange.
     *
     * Utilizado para encaminhar:
     *
     * - mensagens para as filas de retry;
     * - mensagens definitivamente falhadas para as DLQs.
     */
    @Bean
    public DirectExchange dlxExchange() {

        return new DirectExchange(
                DLX_EXCHANGE,
                true,
                false
        );
    }

    // =========================================================
    // MESSAGE CONVERTER
    // =========================================================

    /**
     * Converte automaticamente os objectos Java para JSON
     * e JSON para objectos Java.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {

        return new Jackson2JsonMessageConverter();
    }

    // =========================================================
    // RABBIT TEMPLATE
    // =========================================================

    /**
     * RabbitTemplate utilizado pelos publishers e pelos
     * consumers que precisam de publicar mensagens.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {

        RabbitTemplate rabbitTemplate =
                new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMessageConverter(messageConverter);

        return rabbitTemplate;
    }

    // =========================================================
    // LISTENER CONTAINER
    // =========================================================

    /**
     * Configuração dos consumers RabbitMQ.
     *
     * Os consumers utilizam ACK manual através de:
     *
     *     channel.basicAck(...)
     *     channel.basicReject(...)
     */
    @Bean
    public SimpleRabbitListenerContainerFactory
    rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {

        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);

        factory.setAcknowledgeMode(
                AcknowledgeMode.MANUAL
        );

        return factory;
    }

    // =========================================================
    // EMAIL QUEUE
    // =========================================================

    @Bean
    public Queue emailQueue() {

        return QueueBuilder
                .durable(EMAIL_QUEUE)
                .withArgument(
                        "x-dead-letter-exchange",
                        DLX_EXCHANGE
                )
                .withArgument(
                        "x-dead-letter-routing-key",
                        EMAIL_DLQ_ROUTING_KEY
                )
                .withArgument(
                        "x-max-priority",
                        10
                )
                .build();
    }

    @Bean
    public Binding emailBinding() {

        return BindingBuilder
                .bind(emailQueue())
                .to(notificationExchange())
                .with(EMAIL_ROUTING_KEY);
    }

    // =========================================================
    // EMAIL RETRY
    // =========================================================

    @Bean
    public Queue emailRetryQueue() {

        return QueueBuilder
                .durable(EMAIL_RETRY_QUEUE)
                .withArgument(
                        "x-message-ttl",
                        5000
                )
                .withArgument(
                        "x-dead-letter-exchange",
                        NOTIFICATION_EXCHANGE
                )
                .withArgument(
                        "x-dead-letter-routing-key",
                        EMAIL_ROUTING_KEY
                )
                .build();
    }

    @Bean
    public Binding emailRetryBinding() {

        return BindingBuilder
                .bind(emailRetryQueue())
                .to(dlxExchange())
                .with(EMAIL_RETRY_ROUTING_KEY);
    }

    // =========================================================
    // EMAIL DLQ
    // =========================================================

    @Bean
    public Queue emailDlq() {

        return QueueBuilder
                .durable(EMAIL_DLQ)
                .build();
    }

    @Bean
    public Binding emailDlqBinding() {

        return BindingBuilder
                .bind(emailDlq())
                .to(dlxExchange())
                .with(EMAIL_DLQ_ROUTING_KEY);
    }

    // =========================================================
    // SMS QUEUE
    // =========================================================

    @Bean
    public Queue smsQueue() {

        return QueueBuilder
                .durable(SMS_QUEUE)
                .withArgument(
                        "x-dead-letter-exchange",
                        DLX_EXCHANGE
                )
                .withArgument(
                        "x-dead-letter-routing-key",
                        SMS_DLQ_ROUTING_KEY
                )
                .withArgument(
                        "x-max-priority",
                        10
                )
                .build();
    }

    @Bean
    public Binding smsBinding() {

        return BindingBuilder
                .bind(smsQueue())
                .to(notificationExchange())
                .with(SMS_ROUTING_KEY);
    }

    // =========================================================
    // SMS RETRY
    // =========================================================

    @Bean
    public Queue smsRetryQueue() {

        return QueueBuilder
                .durable(SMS_RETRY_QUEUE)
                .withArgument(
                        "x-message-ttl",
                        5000
                )
                .withArgument(
                        "x-dead-letter-exchange",
                        NOTIFICATION_EXCHANGE
                )
                .withArgument(
                        "x-dead-letter-routing-key",
                        SMS_ROUTING_KEY
                )
                .build();
    }

    @Bean
    public Binding smsRetryBinding() {

        return BindingBuilder
                .bind(smsRetryQueue())
                .to(dlxExchange())
                .with(SMS_RETRY_ROUTING_KEY);
    }

    // =========================================================
    // SMS DLQ
    // =========================================================

    @Bean
    public Queue smsDlq() {

        return QueueBuilder
                .durable(SMS_DLQ)
                .build();
    }

    @Bean
    public Binding smsDlqBinding() {

        return BindingBuilder
                .bind(smsDlq())
                .to(dlxExchange())
                .with(SMS_DLQ_ROUTING_KEY);
    }

    // =========================================================
    // PUSH QUEUE
    // =========================================================

    @Bean
    public Queue pushQueue() {

        return QueueBuilder
                .durable(PUSH_QUEUE)
                .withArgument(
                        "x-dead-letter-exchange",
                        DLX_EXCHANGE
                )
                .withArgument(
                        "x-dead-letter-routing-key",
                        PUSH_DLQ_ROUTING_KEY
                )
                .withArgument(
                        "x-max-priority",
                        10
                )
                .build();
    }

    @Bean
    public Binding pushBinding() {

        return BindingBuilder
                .bind(pushQueue())
                .to(notificationExchange())
                .with(PUSH_ROUTING_KEY);
    }

    // =========================================================
    // PUSH RETRY
    // =========================================================

    @Bean
    public Queue pushRetryQueue() {

        return QueueBuilder
                .durable(PUSH_RETRY_QUEUE)
                .withArgument(
                        "x-message-ttl",
                        5000
                )
                .withArgument(
                        "x-dead-letter-exchange",
                        NOTIFICATION_EXCHANGE
                )
                .withArgument(
                        "x-dead-letter-routing-key",
                        PUSH_ROUTING_KEY
                )
                .build();
    }

    @Bean
    public Binding pushRetryBinding() {

        return BindingBuilder
                .bind(pushRetryQueue())
                .to(dlxExchange())
                .with(PUSH_RETRY_ROUTING_KEY);
    }

    // =========================================================
    // PUSH DLQ
    // =========================================================

    @Bean
    public Queue pushDlq() {

        return QueueBuilder
                .durable(PUSH_DLQ)
                .build();
    }

    @Bean
    public Binding pushDlqBinding() {

        return BindingBuilder
                .bind(pushDlq())
                .to(dlxExchange())
                .with(PUSH_DLQ_ROUTING_KEY);
    }
}