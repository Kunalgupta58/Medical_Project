package com.eldercare.platform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PendingInvitationResponse {
    private UUID id;
    private String elderlyUserName;
    private String inviteeRole;
    private String message;
    private String invitationToken;
    private Instant expiresAt;
    private Instant createdAt;
}
