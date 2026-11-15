package com.eldercare.platform.scheduler;

import com.eldercare.platform.entity.CaregiverRelationship;
import com.eldercare.platform.entity.Medication;
import com.eldercare.platform.entity.MedicationSchedule;
import com.eldercare.platform.enums.DoseStatus;
import com.eldercare.platform.enums.NotificationType;
import com.eldercare.platform.repository.CaregiverRelationshipRepository;
import com.eldercare.platform.repository.MedicationRepository;
import com.eldercare.platform.repository.MedicationScheduleRepository;
import com.eldercare.platform.service.MedicationService;
import com.eldercare.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MedicationReminderScheduler {

    // Escalation threshold: doses pending 30+ minutes without confirmation trigger caregiver alert
    private static final long ESCALATION_THRESHOLD_MINUTES = 30;

    private final MedicationScheduleRepository scheduleRepository;
    private final MedicationRepository medicationRepository;
    private final CaregiverRelationshipRepository relationshipRepository;
    private final NotificationService notificationService;
    private final MedicationService medicationService;

    /**
     * Generate tomorrow's dose records each night.
     * Runs at 23:30 each day.
     */
    @Scheduled(cron = "0 30 23 * * *")
    @Transactional
    public void generateTomorrowSchedules() {
        log.info("Generating medication schedules for tomorrow");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Medication> activeMeds = medicationRepository.findAll()
                .stream()
                .filter(Medication::isActive)
                .filter(m -> m.getEndDate() == null || !m.getEndDate().isBefore(tomorrow))
                .filter(m -> !m.getStartDate().isAfter(tomorrow))
                .toList();

        for (Medication med : activeMeds) {
            try {
                medicationService.generateSchedulesForMedication(med, tomorrow, tomorrow);
            } catch (Exception e) {
                log.error("Error generating schedules for medication {}: {}", med.getId(), e.getMessage());
            }
        }
        log.info("Finished generating schedules for {} medications", activeMeds.size());
    }

    /**
     * Send medication reminders every 5 minutes.
     * Finds PENDING doses whose scheduled time is within the next 5 minutes.
     */
    @Scheduled(fixedDelay = 300_000) // every 5 minutes
    @Transactional
    public void sendMedicationReminders() {
        Instant now = Instant.now();
        Instant windowEnd = now.plusSeconds(300);

        // Find doses scheduled in the next 5 minutes that are still PENDING
        List<MedicationSchedule> pendingDoses = scheduleRepository.findAll()
                .stream()
                .filter(s -> s.getStatus() == DoseStatus.PENDING)
                .filter(s -> !s.getScheduledDatetime().isBefore(now)
                        && s.getScheduledDatetime().isBefore(windowEnd))
                .toList();

        for (MedicationSchedule dose : pendingDoses) {
            try {
                notificationService.createNotification(
                        dose.getElderlyUser().getId(),
                        "Medication Reminder: " + dose.getMedication().getName(),
                        "It's time to take " + dose.getMedication().getName()
                                + " — " + dose.getMedication().getDosageText()
                                + ". Please confirm once taken.",
                        NotificationType.MEDICATION_REMINDER,
                        "MedicationSchedule",
                        dose.getId()
                );
            } catch (Exception e) {
                log.error("Error sending reminder for dose {}: {}", dose.getId(), e.getMessage());
            }
        }
    }

    /**
     * Escalate unconfirmed doses to caregivers.
     * Doses pending more than ESCALATION_THRESHOLD_MINUTES without confirmation.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void escalateUnconfirmedDoses() {
        Instant threshold = Instant.now().minusSeconds(ESCALATION_THRESHOLD_MINUTES * 60);
        List<MedicationSchedule> unconfirmed = scheduleRepository.findUnconfirmedForEscalation(threshold);

        for (MedicationSchedule dose : unconfirmed) {
            try {
                // Mark as UNCONFIRMED
                dose.setStatus(DoseStatus.UNCONFIRMED);
                dose.setEscalationSent(true);
                dose.setEscalationSentAt(Instant.now());
                scheduleRepository.save(dose);

                // Notify all authorized caregivers and family members
                List<CaregiverRelationship> relationships = relationshipRepository
                        .findByElderlyUserIdAndActiveTrue(dose.getElderlyUser().getId());

                for (CaregiverRelationship rel : relationships) {
                    notificationService.createNotification(
                            rel.getRelatedUser().getId(),
                            "⚠ Medication Not Confirmed: " + dose.getMedication().getName(),
                            dose.getElderlyUser().getFullName()
                                    + " has not confirmed taking "
                                    + dose.getMedication().getName()
                                    + " (" + dose.getMedication().getDosageText() + ")."
                                    + " Scheduled at "
                                    + dose.getScheduledDatetime()
                                    + ". Please follow up.",
                            NotificationType.CAREGIVER_ESCALATION,
                            "MedicationSchedule",
                            dose.getId()
                    );
                }

                log.info("Escalated unconfirmed dose {} for elderly {}",
                        dose.getId(), dose.getElderlyUser().getId());

            } catch (Exception e) {
                log.error("Error escalating dose {}: {}", dose.getId(), e.getMessage());
            }
        }
    }

    /**
     * Send appointment reminders for appointments 24 hours away.
     * Runs every hour.
     */
    @Scheduled(fixedDelay = 3_600_000)
    public void sendAppointmentReminders() {
        // Appointment reminder logic is in AppointmentReminderScheduler
    }
}
