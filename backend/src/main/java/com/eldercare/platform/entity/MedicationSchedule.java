package com.eldercare.platform.entity;

import com.eldercare.platform.enums.DoseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "medication_schedules",
       uniqueConstraints = @UniqueConstraint(columnNames = {"medication_id", "scheduled_datetime"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elderly_user_id", nullable = false)
    private User elderlyUser;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time", nullable = false)
    private LocalTime scheduledTime;

    @Column(name = "scheduled_datetime", nullable = false)
    private Instant scheduledDatetime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DoseStatus status = DoseStatus.PENDING;

    @Column(name = "taken_at")
    private Instant takenAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_user_id")
    private User confirmedBy;

    @Column(name = "snooze_count", nullable = false)
    @Builder.Default
    private int snoozeCount = 0;

    @Column(name = "snoozed_until")
    private Instant snoozedUntil;

    @Column(name = "skip_reason")
    private String skipReason;

    @Column(name = "escalation_sent", nullable = false)
    @Builder.Default
    private boolean escalationSent = false;

    @Column(name = "escalation_sent_at")
    private Instant escalationSentAt;

    @Column(name = "escalation_acknowledged", nullable = false)
    @Builder.Default
    private boolean escalationAcknowledged = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escalation_acknowledged_by")
    private User escalationAcknowledgedBy;

    @Column(name = "escalation_acknowledged_at")
    private Instant escalationAcknowledgedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
