package com.github.alfredobaptista.notification.domain.entity;

import com.github.alfredobaptista.notification.domain.enums.*;
import com.github.alfredobaptista.notification.domain.exception.InvalidNotificationException;
import com.github.alfredobaptista.notification.domain.exception.InvalidNotificationStateException;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Notification {

    private final UUID id;

    private final NotificationChannel channel;

    private final String recipient;

    private final String subject;

    private final String content;

    private NotificationStatus status;

    private final NotificationPriority priority;

    private int attempts;

    private final String idempotencyKey;

    private final OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private OffsetDateTime processedAt;

    private String lastError;

    /**
     * Cria uma nova notificação.
     */
    public Notification(
            NotificationChannel channel,
            String recipient,
            String subject,
            String content,
            NotificationPriority priority,
            String idempotencyKey
    ) {
        validateCreationData(
                channel,
                recipient,
                content
        );

        this.id = UUID.randomUUID();
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.content = content;
        this.status = NotificationStatus.PENDING;
        this.priority = priority != null
                ? priority
                : NotificationPriority.NORMAL;
        this.attempts = 0;
        this.idempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * Reconstrói uma notificação existente a partir da persistência.
     *
     * Este construtor é utilizado pelo adapter de persistência.
     */
    public Notification(
            UUID id,
            NotificationChannel channel,
            String recipient,
            String subject,
            String content,
            NotificationStatus status,
            NotificationPriority priority,
            int attempts,
            String idempotencyKey,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime processedAt,
            String lastError
    ) {
        validateReconstitutionData(
                id,
                channel,
                recipient,
                content,
                status,
                priority,
                attempts,
                createdAt,
                updatedAt
        );

        this.id = id;
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.content = content;
        this.status = status;
        this.priority = priority;
        this.attempts = attempts;
        this.idempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.processedAt = processedAt;
        this.lastError = lastError;
    }

    // =========================================================
    // Domain Behaviour
    // =========================================================

    /**
     * Coloca a notificação em processamento e regista uma tentativa.
     *
     * Transições permitidas:
     *
     * PENDING   -> PROCESSING
     * RETRYING  -> PROCESSING
     */
    public void markAsProcessing() {

        if (status != NotificationStatus.PENDING
                && status != NotificationStatus.RETRYING) {

            throw new InvalidNotificationStateException(
                    status,
                    "process notification"
            );
        }

        this.status = NotificationStatus.PROCESSING;
        this.attempts++;
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Marca a notificação como entregue com sucesso.
     *
     * Transição permitida:
     *
     * PROCESSING -> DELIVERED
     */
    public void markAsDelivered() {

        if (status != NotificationStatus.PROCESSING) {

            throw new InvalidNotificationStateException(
                    status,
                    "mark notification as delivered"
            );
        }

        this.status = NotificationStatus.DELIVERED;
        this.processedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        this.lastError = null;
    }

    /**
     * Marca a notificação para uma nova tentativa.
     *
     * Transição permitida:
     *
     * PROCESSING -> RETRYING
     */
    public void markAsRetrying(String error) {

        if (status != NotificationStatus.PROCESSING) {

            throw new InvalidNotificationStateException(
                    status,
                    "mark notification for retry"
            );
        }

        this.status = NotificationStatus.RETRYING;
        this.lastError = normalizeError(error);
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Move a notificação para Dead Letter.
     *
     * Transições permitidas:
     *
     * PROCESSING -> DEAD_LETTER
     * RETRYING   -> DEAD_LETTER
     */
    public void markAsDeadLetter(String error) {

        if (status != NotificationStatus.PROCESSING
                && status != NotificationStatus.RETRYING) {

            throw new InvalidNotificationStateException(
                    status,
                    "move notification to dead letter"
            );
        }

        this.status = NotificationStatus.DEAD_LETTER;
        this.lastError = normalizeError(error);
        this.updatedAt = OffsetDateTime.now();
    }

    // =========================================================
    // Validation
    // =========================================================

    private static void validateCreationData(
            NotificationChannel channel,
            String recipient,
            String content
    ) {
        if (channel == null) {
            throw new InvalidNotificationException(
                    "Notification channel is required"
            );
        }

        if (recipient == null || recipient.isBlank()) {
            throw new InvalidNotificationException(
                    "Notification recipient is required"
            );
        }

        if (content == null || content.isBlank()) {
            throw new InvalidNotificationException(
                    "Notification content is required"
            );
        }
    }

    private static void validateReconstitutionData(
            UUID id,
            NotificationChannel channel,
            String recipient,
            String content,
            NotificationStatus status,
            NotificationPriority priority,
            int attempts,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        if (id == null) {
            throw new InvalidNotificationException(
                    "Notification id is required"
            );
        }

        if (channel == null) {
            throw new InvalidNotificationException(
                    "Notification channel is required"
            );
        }

        if (recipient == null || recipient.isBlank()) {
            throw new InvalidNotificationException(
                    "Notification recipient is required"
            );
        }

        if (content == null || content.isBlank()) {
            throw new InvalidNotificationException(
                    "Notification content is required"
            );
        }

        if (status == null) {
            throw new InvalidNotificationException(
                    "Notification status is required"
            );
        }

        if (priority == null) {
            throw new InvalidNotificationException(
                    "Notification priority is required"
            );
        }

        if (attempts < 0) {
            throw new InvalidNotificationException(
                    "Notification attempts cannot be negative"
            );
        }

        if (createdAt == null) {
            throw new InvalidNotificationException(
                    "Notification createdAt is required"
            );
        }

        if (updatedAt == null) {
            throw new InvalidNotificationException(
                    "Notification updatedAt is required"
            );
        }
    }

    private static String normalizeIdempotencyKey(String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }

        return idempotencyKey.trim();
    }

    private static String normalizeError(String error) {

        if (error == null || error.isBlank()) {
            return "Unknown notification processing error";
        }

        return error.trim();
    }

    // =========================================================
    // Getters
    // =========================================================

    public UUID getId() {
        return id;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public NotificationPriority getPriority() {
        return priority;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public String getLastError() {
        return lastError;
    }
}