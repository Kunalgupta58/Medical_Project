package com.eldercare.platform.service;

import com.eldercare.platform.entity.Medication;
import com.eldercare.platform.entity.MedicationSchedule;
import com.eldercare.platform.entity.User;
import com.eldercare.platform.enums.UserRole;
import com.eldercare.platform.exception.UnauthorizedException;
import com.eldercare.platform.repository.CaregiverRelationshipRepository;
import com.eldercare.platform.repository.MedicationRepository;
import com.eldercare.platform.repository.MedicationScheduleRepository;
import com.eldercare.platform.repository.UserRepository;
import com.eldercare.platform.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicationServiceTest {

    @Mock
    private MedicationRepository medicationRepository;

    @Mock
    private MedicationScheduleRepository scheduleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaregiverRelationshipRepository relationshipRepository;

    private MedicationService medicationService;

    @BeforeEach
    void setUp() {
        medicationService = new MedicationService(
                medicationRepository, scheduleRepository, userRepository,
                relationshipRepository, new ObjectMapper());
    }

    @Test
    void generateSchedulesChecksExistingInstantsInOneQueryAndSkipsExistingAndDuplicateTimes() {
        UUID medicationId = UUID.randomUUID();
        LocalDate firstDay = LocalDate.of(2026, 1, 1);
        LocalDate lastDay = firstDay.plusDays(1);
        ZoneId zone = ZoneId.systemDefault();
        Instant existingDose = firstDay.atTime(8, 0).atZone(zone).toInstant();
        Medication medication = Medication.builder()
                .id(medicationId)
                .elderlyUser(User.builder().id(UUID.randomUUID()).build())
                .active(true)
                .startDate(firstDay)
                .scheduledTimesJson("[\"08:00\", \"20:00\", \"08:00\"]")
                .build();

        when(scheduleRepository.findScheduledDatetimesByMedicationIdAndRange(
                medicationId, firstDay.atStartOfDay(zone).toInstant(),
                lastDay.plusDays(1).atStartOfDay(zone).toInstant()))
                .thenReturn(List.of(existingDose));

        medicationService.generateSchedulesForMedication(medication, firstDay, lastDay);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<MedicationSchedule>> schedulesCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(scheduleRepository).saveAll(schedulesCaptor.capture());
        List<MedicationSchedule> created = java.util.stream.StreamSupport
                .stream(schedulesCaptor.getValue().spliterator(), false).toList();

        assertEquals(3, created.size());
        assertEquals(List.of(
                firstDay.atTime(20, 0).atZone(zone).toInstant(),
                lastDay.atTime(8, 0).atZone(zone).toInstant(),
                lastDay.atTime(20, 0).atZone(zone).toInstant()),
                created.stream().map(MedicationSchedule::getScheduledDatetime).toList());
        verify(scheduleRepository).findScheduledDatetimesByMedicationIdAndRange(
                medicationId, firstDay.atStartOfDay(zone).toInstant(),
                lastDay.plusDays(1).atStartOfDay(zone).toInstant());
        verify(scheduleRepository, never()).findByMedicationIdAndScheduledDatetime(
                org.mockito.ArgumentMatchers.eq(medicationId),
                org.mockito.ArgumentMatchers.any(Instant.class));
    }

    @Test
    void generateSchedulesDoesNotQueryWhenMedicationIsInactive() {
        Medication medication = Medication.builder()
                .id(UUID.randomUUID())
                .active(false)
                .startDate(LocalDate.of(2026, 1, 1))
                .build();

        medicationService.generateSchedulesForMedication(
                medication, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2));

        verify(scheduleRepository, never()).findScheduledDatetimesByMedicationIdAndRange(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(scheduleRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void adminCannotUpdateMedication() {
        UUID adminId = UUID.randomUUID();
        Medication medication = Medication.builder()
                .id(UUID.randomUUID())
                .elderlyUser(User.builder().id(UUID.randomUUID()).build())
                .build();
        User admin = User.builder()
                .id(adminId)
                .email("admin@example.com")
                .passwordHash("hash")
                .role(UserRole.ADMIN)
                .active(true)
                .build();

        when(medicationRepository.findById(medication.getId()))
                .thenReturn(java.util.Optional.of(medication));
        when(userRepository.findById(adminId)).thenReturn(java.util.Optional.of(admin));

        assertThrows(UnauthorizedException.class, () -> medicationService.updateMedication(
                medication.getId(), new com.eldercare.platform.dto.request.MedicationRequest(),
                UserPrincipal.create(admin)));
        verify(medicationRepository, never()).save(medication);
    }
}
