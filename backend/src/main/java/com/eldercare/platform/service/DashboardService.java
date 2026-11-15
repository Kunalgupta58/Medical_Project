package com.eldercare.platform.service;

import com.eldercare.platform.dto.response.AppointmentResponse;
import com.eldercare.platform.dto.response.DashboardResponse;
import com.eldercare.platform.dto.response.NotificationResponse;
import com.eldercare.platform.entity.CaregiverRelationship;
import com.eldercare.platform.entity.MedicationSchedule;
import com.eldercare.platform.enums.DoseStatus;
import com.eldercare.platform.enums.UserRole;
import com.eldercare.platform.exception.BadRequestException;
import com.eldercare.platform.exception.UnauthorizedException;
import com.eldercare.platform.repository.*;
import com.eldercare.platform.entity.User;
import com.eldercare.platform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final MedicationRepository medicationRepository;
    private final MedicationScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final CareTaskRepository careTaskRepository;
    private final MedicalDocumentRepository documentRepository;
    private final NotificationRepository notificationRepository;
    private final CaregiverRelationshipRepository relationshipRepository;
    private final AppointmentService appointmentService;

    public DashboardResponse getOverview(UserPrincipal principal, UUID elderlyUserId) {
        UUID eid = resolveElderlyUserId(principal, elderlyUserId);

        LocalDate today = LocalDate.now();
        List<MedicationSchedule> todayDoses = scheduleRepository.findTodaySchedule(eid, today);

        long totalToday = todayDoses.size();
        long takenToday = todayDoses.stream().filter(d -> d.getStatus() == DoseStatus.TAKEN).count();
        long skippedToday = todayDoses.stream().filter(d -> d.getStatus() == DoseStatus.SKIPPED).count();
        long pendingToday = todayDoses.stream().filter(d -> d.getStatus() == DoseStatus.PENDING || d.getStatus() == DoseStatus.SNOOZED).count();
        long unconfirmedToday = todayDoses.stream().filter(d -> d.getStatus() == DoseStatus.UNCONFIRMED).count();

        // 7-day adherence
        LocalDate weekAgo = today.minusDays(6);
        long scheduled7 = scheduleRepository.countScheduledInPeriod(eid, weekAgo, today);
        long taken7 = scheduleRepository.countTakenInPeriod(eid, weekAgo, today);
        double adherence = scheduled7 > 0 ? (double) taken7 / scheduled7 * 100 : 0;

        // Weekly chart data (last 7 days)
        List<Map<String, Object>> weeklyAdherence = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            List<MedicationSchedule> dayDoses = scheduleRepository.findByElderlyUserIdAndScheduledDate(eid, day);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", day.toString());
            entry.put("scheduled", dayDoses.size());
            entry.put("taken", dayDoses.stream().filter(d -> d.getStatus() == DoseStatus.TAKEN).count());
            entry.put("missed", dayDoses.stream().filter(d ->
                    d.getStatus() == DoseStatus.SKIPPED || d.getStatus() == DoseStatus.UNCONFIRMED).count());
            weeklyAdherence.add(entry);
        }

        long upcomingAppointments = appointmentRepository.countByElderlyUserIdAndStatus(eid, "SCHEDULED");
        long pendingTasks = careTaskRepository.countByElderlyUserIdAndStatus(eid, "PENDING")
                + careTaskRepository.countByElderlyUserIdAndStatus(eid, "IN_PROGRESS");
        long totalDocs = documentRepository.countByElderlyUserIdAndDeletedFalse(eid);
        long activeMeds = medicationRepository.countByElderlyUserIdAndActiveTrue(eid);
        long unread = notificationRepository.countByUserIdAndReadFalse(principal.getId());

        List<AppointmentResponse> nextAppts = appointmentRepository
                .findUpcoming(eid, Instant.now())
                .stream().limit(3)
                .map(a -> AppointmentResponse.builder()
                        .id(a.getId())
                        .doctorName(a.getDoctorName())
                        .hospitalName(a.getHospitalName())
                        .appointmentDatetime(a.getAppointmentDatetime())
                        .purpose(a.getPurpose())
                        .status(a.getStatus())
                        .build())
                .collect(Collectors.toList());

        List<NotificationResponse> recentNotifs = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(principal.getId(), PageRequest.of(0, 5))
                .stream()
                .map(n -> NotificationResponse.builder()
                        .id(n.getId())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .notificationType(n.getNotificationType())
                        .read(n.isRead())
                        .createdAt(n.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return DashboardResponse.builder()
                .totalScheduledToday(totalToday)
                .takenToday(takenToday)
                .skippedToday(skippedToday)
                .pendingToday(pendingToday)
                .unconfirmedToday(unconfirmedToday)
                .upcomingAppointments(upcomingAppointments)
                .pendingCareTasks(pendingTasks)
                .activeMedications(activeMeds)
                .totalDocuments(totalDocs)
                .unreadNotifications(unread)
                .adherencePercentage(Math.round(adherence * 10.0) / 10.0)
                .weeklyAdherence(weeklyAdherence)
                .nextAppointments(nextAppts)
                .recentNotifications(recentNotifs)
                .build();
    }

    private UUID resolveElderlyUserId(UserPrincipal principal, UUID requestedId) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        if (user.getRole() == UserRole.ELDERLY) return user.getId();
        if (requestedId != null) {
            boolean hasAccess = user.getRole() == UserRole.ADMIN ||
                    relationshipRepository.existsByElderlyUserIdAndRelatedUserIdAndActiveTrue(requestedId, principal.getId());
            if (!hasAccess) throw new UnauthorizedException("No authorized access to this profile");
            return requestedId;
        }
        if (user.getRole() == UserRole.ADMIN) {
            throw new BadRequestException("elderlyUserId is required for admin access");
        }

        List<CaregiverRelationship> relationships = relationshipRepository
                .findByRelatedUserIdAndActiveTrue(principal.getId());

        if (relationships.size() == 1) {
            return relationships.get(0).getElderlyUser().getId();
        }
        if (relationships.size() > 1) {
            throw new BadRequestException("Multiple elderly profiles are connected; please specify elderlyUserId");
        }
        throw new BadRequestException("elderlyUserId is required");
    }
}
