package com.github.alfredobaptista.notification.adapter.in.web.dto;

import com.github.alfredobaptista.notification.domain.enums.NotificationChannel;
import com.github.alfredobaptista.notification.domain.enums.NotificationPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class NotificationRequestDto {

    @NotNull(message = "O canal é obrigatório")
    private NotificationChannel channel;

    @NotBlank(message = "O destinatário é obrigatório")
    private String recipient;

    private String subject;

    @NotBlank(message = "O conteúdo é obrigatório")
    private String content;

    private NotificationPriority priority;
}