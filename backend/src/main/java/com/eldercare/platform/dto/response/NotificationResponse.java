package com.eldercare.platform.dto.response;

import com.eldercare.platform.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private String title;
    private String message;
    private NotificationType notificationType;
    private String relatedEntityType;
    private UUID relatedEntityId;
    private boolean read;
    private Instant readAt;
    private boolean emailSent;
    private Instant createdAt;
}
