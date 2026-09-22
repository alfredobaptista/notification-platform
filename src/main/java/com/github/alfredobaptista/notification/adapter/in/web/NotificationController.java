package com.github.alfredobaptista.notification.adapter.in.web;

import com.github.alfredobaptista.notification.adapter.in.web.dto.NotificationRequestDto;
import com.github.alfredobaptista.notification.adapter.in.web.dto.NotificationResponseDto;
import com.github.alfredobaptista.notification.application.port.in.CreateNotificationUseCase;
import com.github.alfredobaptista.notification.application.port.in.GetNotificationUseCase;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final CreateNotificationUseCase createNotificationUseCase;
    private final GetNotificationUseCase getNotificationUseCase;

    @PostMapping
    public ResponseEntity<NotificationResponseDto> createNotification(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestBody @Valid NotificationRequestDto request
    ) {

        var notification = createNotificationUseCase.execute(
                request.getChannel(),
                request.getRecipient(),
                request.getSubject(),
                request.getContent(),
                request.getPriority(),
                idempotencyKey
        );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(NotificationResponseDto.fromDomain(notification));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponseDto> getNotification(
            @PathVariable UUID id
    ) {

        return getNotificationUseCase.execute(id)
                .map(NotificationResponseDto::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}