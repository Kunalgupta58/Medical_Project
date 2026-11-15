package com.eldercare.platform.repository;

import com.eldercare.platform.entity.Medication;
import com.eldercare.platform.entity.MedicationSchedule;
import com.eldercare.platform.entity.User;
import com.eldercare.platform.enums.DoseStatus;
import com.eldercare.platform.enums.MedicineFrequency;
import com.eldercare.platform.enums.MedicineType;
import com.eldercare.platform.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MedicationScheduleRepositoryTest {

    @Autowired
    private MedicationScheduleRepository scheduleRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private UserRepository userRepository;

    private User elderlyUser;

    @BeforeEach
    void setUp() {
        elderlyUser = userRepository.saveAndFlush(User.builder()
                .email(UUID.randomUUID() + "@example.com")
                .passwordHash("hash")
                .firstName("Ava")
                .lastName("Patel")
                .role(UserRole.ELDERLY)
                .active(true)
                .build());
    }

    @Test
    void pendingReminderQueryFiltersByTimeAndStatusAndFetchesMedicationDetails() {
        Medication medication = saveMedication(LocalDate.now().minusDays(1), null, true);
        Instant now = Instant.now();
        MedicationSchedule due = saveSchedule(medication, now.plusSeconds(60), DoseStatus.PENDING);
        saveSchedule(medication, now.plusSeconds(600), DoseStatus.PENDING);
        saveSchedule(medication, now.plusSeconds(120), DoseStatus.TAKEN);

        List<MedicationSchedule> reminders =
                scheduleRepository.findPendingReminders(now, now.plusSeconds(300));

        assertEquals(1, reminders.size());
        assertEquals(due.getId(), reminders.get(0).getId());
        assertEquals("Metformin", reminders.get(0).getMedication().getName());
        assertEquals("Ava Patel", reminders.get(0).getElderlyUser().getFullName());
    }

    @Test
    void activeMedicationQueryFiltersInTheDatabaseByDateAndStatus() {
        Medication active = saveMedication(LocalDate.now().minusDays(1), null, true);
        saveMedication(LocalDate.now().plusDays(2), null, true);
        saveMedication(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1), true);
        saveMedication(LocalDate.now().minusDays(1), null, false);

        List<Medication> activeForTomorrow =
                medicationRepository.findAllActiveForDate(LocalDate.now().plusDays(1));

        assertEquals(List.of(active.getId()), activeForTomorrow.stream().map(Medication::getId).toList());
        assertEquals("Ava Patel", activeForTomorrow.get(0).getElderlyUser().getFullName());
    }

    private Medication saveMedication(LocalDate startDate, LocalDate endDate, boolean active) {
        return medicationRepository.saveAndFlush(Medication.builder()
                .elderlyUser(elderlyUser)
                .createdBy(elderlyUser)
                .name("Metformin")
                .dosageText("500mg")
                .medicineType(MedicineType.TABLET)
                .frequency(MedicineFrequency.ONCE_DAILY)
                .startDate(startDate)
                .endDate(endDate)
                .scheduledTimesJson("[\"08:00\"]")
                .active(active)
                .build());
    }

    private MedicationSchedule saveSchedule(Medication medication, Instant scheduledAt, DoseStatus status) {
        return scheduleRepository.saveAndFlush(MedicationSchedule.builder()
                .medication(medication)
                .elderlyUser(elderlyUser)
                .scheduledDate(LocalDate.now())
                .scheduledTime(LocalTime.now())
                .scheduledDatetime(scheduledAt)
                .status(status)
                .build());
    }
}
