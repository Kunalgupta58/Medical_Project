package com.eldercare.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class AppointmentRequest {

    @NotBlank(message = "Doctor name is required")
    private String doctorName;

    private String hospitalName;

    @NotNull(message = "Appointment date and time is required")
    private Instant appointmentDatetime;

    @NotBlank(message = "Purpose is required")
    private String purpose;

    private String notes;

    private UUID elderlyUserId;
}
