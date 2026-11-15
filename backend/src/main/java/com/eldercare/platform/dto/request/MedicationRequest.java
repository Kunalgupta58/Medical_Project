package com.eldercare.platform.dto.request;

import com.eldercare.platform.enums.MedicineFrequency;
import com.eldercare.platform.enums.MedicineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class MedicationRequest {

    @NotBlank(message = "Medicine name is required")
    private String name;

    private String genericName;

    @NotBlank(message = "Dosage is required")
    private String dosageText;

    @NotNull(message = "Medicine type is required")
    private MedicineType medicineType;

    @NotNull(message = "Frequency is required")
    private MedicineFrequency frequency;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private List<String> scheduledTimes;

    private String instructions;
    private String prescribingDoctor;
    private String notes;
    private String color;

    // For family/caregiver adding medication for an elderly user
    private UUID elderlyUserId;
}
