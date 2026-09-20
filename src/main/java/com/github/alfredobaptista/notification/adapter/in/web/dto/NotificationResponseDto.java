package com.github.alfredobaptista.notification.adapter.in.web.dto;

import com.github.alfredobaptista.notification.domain.entity.Notification;
import com.github.alfredobaptista.notification.domain.enums.NotificationChannel;
import com.github.alfredobaptista.notification.domain.enums.NotificationStatus;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class NotificationResponseDto {

    private UUID id;
    private NotificationChannel channel;
    private String recipient;
    private NotificationStatus status;
    private Integer attempts;
    private OffsetDateTime createdAt;
    private OffsetDateTime processedAt;

    public static NotificationResponseDto fromDomain(Notification notification) {

        var dto = new NotificationResponseDto();

        dto.setId(notification.getId());
        dto.setChannel(notification.getChannel());
        dto.setRecipient(notification.getRecipient());
        dto.setStatus(notification.getStatus());
        dto.setAttempts(notification.getAttempts());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setProcessedAt(notification.getProcessedAt());

        return dto;
    }
}