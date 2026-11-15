package com.eldercare.platform.dto.response;

import com.eldercare.platform.enums.DoseStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class DoseResponse {
    private UUID id;
    private UUID medicationId;
    private String medicationName;
    private String dosageText;
    private String medicineType;
    private String color;
    private String instructions;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private Instant scheduledDatetime;
    private DoseStatus status;
    private Instant takenAt;
    private String confirmedByName;
    private int snoozeCount;
    private Instant snoozedUntil;
    private String skipReason;
    private boolean escalationSent;
    private boolean escalationAcknowledged;
}
