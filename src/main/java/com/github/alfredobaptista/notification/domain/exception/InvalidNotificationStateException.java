package com.github.alfredobaptista.notification.domain.exception;

import com.github.alfredobaptista.notification.domain.enums.NotificationStatus;

public class InvalidNotificationStateException extends DomainException {

    public InvalidNotificationStateException(
            NotificationStatus currentStatus,
            String operation
    ) {
        super(
            "Cannot " + operation +
            " notification from status: " + currentStatus
        );
    }
}