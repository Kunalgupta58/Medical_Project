package com.eldercare.platform.dto.response;

import com.eldercare.platform.enums.MedicineFrequency;
import com.eldercare.platform.enums.MedicineType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MedicationResponse {
    private UUID id;
    private UUID elderlyUserId;
    private String elderlyUserName;
    private String name;
    private String genericName;
    private String dosageText;
    private MedicineType medicineType;
    private MedicineFrequency frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> scheduledTimes;
    private String instructions;
    private String prescribingDoctor;
    private String notes;
    private boolean active;
    private String color;
    private Instant createdAt;
    private Instant updatedAt;
}
