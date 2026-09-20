package com.github.alfredobaptista.notification.adapter.in.web.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> errors
) {

    public record FieldError(
            String field,
            String message
    ) {
    }
}