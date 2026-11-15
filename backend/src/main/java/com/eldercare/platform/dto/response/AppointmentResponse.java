package com.eldercare.platform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AppointmentResponse {
    private UUID id;
    private UUID elderlyUserId;
    private String elderlyUserName;
    private String doctorName;
    private String hospitalName;
    private Instant appointmentDatetime;
    private String purpose;
    private String notes;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
