package com.eldercare.platform.entity;

import com.eldercare.platform.enums.MedicineFrequency;
import com.eldercare.platform.enums.MedicineType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "medications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elderly_user_id", nullable = false)
    private User elderlyUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private String name;

    @Column(name = "generic_name")
    private String genericName;

    @Column(name = "dosage_text", nullable = false)
    private String dosageText;

    @Enumerated(EnumType.STRING)
    @Column(name = "medicine_type", nullable = false)
    private MedicineType medicineType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MedicineFrequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "scheduled_times", columnDefinition = "jsonb")
    private String scheduledTimesJson;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "prescribing_doctor")
    private String prescribingDoctor;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(length = 7)
    @Builder.Default
    private String color = "#3B82F6";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
