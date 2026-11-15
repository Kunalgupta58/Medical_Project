package com.eldercare.platform.service;

import com.eldercare.platform.dto.response.DashboardResponse;
import com.eldercare.platform.entity.MedicationSchedule;
import com.eldercare.platform.entity.User;
import com.eldercare.platform.enums.DoseStatus;
import com.eldercare.platform.enums.UserRole;
import com.eldercare.platform.repository.AppointmentRepository;
import com.eldercare.platform.repository.CareTaskRepository;
import com.eldercare.platform.repository.CaregiverRelationshipRepository;
import com.eldercare.platform.repository.MedicalDocumentRepository;
import com.eldercare.platform.repository.MedicationRepository;
import com.eldercare.platform.repository.MedicationScheduleRepository;
import com.eldercare.platform.repository.NotificationRepository;
import com.eldercare.platform.repository.UserRepository;
import com.eldercare.platform.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private MedicationRepository medicationRepository;
    @Mock private MedicationScheduleRepository scheduleRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private CareTaskRepository careTaskRepository;
    @Mock private MedicalDocumentRepository documentRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private CaregiverRelationshipRepository relationshipRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                userRepository, medicationRepository, scheduleRepository, appointmentRepository,
                careTaskRepository, documentRepository, notificationRepository,
                relationshipRepository, null);
    }

    @Test
    void weeklyAdherenceLoadsTheWholeWeekWithOneScheduleQuery() {
        UUID userId = UUID.randomUUID();
        User elderlyUser = User.builder()
                .id(userId)
                .email("elder@example.com")
                .passwordHash("hash")
                .role(UserRole.ELDERLY)
                .active(true)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(elderlyUser));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(scheduleRepository.findByPeriod(
                eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        MedicationSchedule.builder()
                                .scheduledDate(LocalDate.now())
                                .status(DoseStatus.TAKEN)
                                .build(),
                        MedicationSchedule.builder()
                                .scheduledDate(LocalDate.now())
                                .status(DoseStatus.SKIPPED)
                                .build()));

        DashboardResponse response = dashboardService.getOverview(UserPrincipal.create(elderlyUser), null);
        Map<String, Object> today = response.getWeeklyAdherence().get(6);

        assertEquals(2, today.get("scheduled"));
        assertEquals(1L, today.get("taken"));
        assertEquals(1L, today.get("missed"));
        verify(scheduleRepository).findByPeriod(
                eq(userId), any(LocalDate.class), any(LocalDate.class));
        verifyNoInteractions(relationshipRepository);
    }
}
