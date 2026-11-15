package com.eldercare.platform.service;

import com.eldercare.platform.dto.response.NotificationResponse;
import com.eldercare.platform.entity.*;
import com.eldercare.platform.enums.UserRole;
import com.eldercare.platform.exception.BadRequestException;
import com.eldercare.platform.exception.ResourceNotFoundException;
import com.eldercare.platform.exception.UnauthorizedException;
import com.eldercare.platform.repository.*;
import com.eldercare.platform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CaregiverService {

    private final CareTaskRepository careTaskRepository;
    private final CaregiverRelationshipRepository relationshipRepository;
    private final MedicationScheduleRepository scheduleRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public List<CareTask> getMyTasks(UserPrincipal principal) {
        return careTaskRepository.findActiveTasks(principal.getId());
    }

    public List<CareTask> getTasksForElderly(UUID elderlyUserId, UserPrincipal principal) {
        checkCaregiverAccess(elderlyUserId, principal);
        return careTaskRepository.findByElderlyUserIdOrderByCreatedAtDesc(elderlyUserId);
    }

    @Transactional
    public CareTask createTask(UUID elderlyUserId, String title, String description,
                                String taskType, String priority, String dueDate,
                                UUID assignedToUserId, UserPrincipal principal) {
        checkCaregiverAccess(elderlyUserId, principal);

        User elderlyUser = userRepository.findById(elderlyUserId).orElseThrow();
        User creator = userRepository.findById(principal.getId()).orElseThrow();
        User assignedTo = assignedToUserId != null
                ? userRepository.findById(assignedToUserId).orElse(null) : null;

        CareTask task = CareTask.builder()
                .elderlyUser(elderlyUser)
                .createdBy(creator)
                .assignedTo(assignedTo != null ? assignedTo : creator)
                .title(title)
                .description(description)
                .taskType(taskType != null ? taskType : "GENERAL_CARE")
                .priority(priority != null ? priority : "MEDIUM")
                .status("PENDING")
                .dueDate(dueDate != null ? java.time.LocalDate.parse(dueDate) : null)
                .build();

        return careTaskRepository.save(task);
    }

    @Transactional
    public CareTask updateTask(UUID taskId, String status, String completionNotes, UserPrincipal principal) {
        CareTask task = careTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        // Only assigned user, creator, or related caregiver can update
        boolean canUpdate = task.getAssignedTo() != null && task.getAssignedTo().getId().equals(principal.getId())
                || task.getCreatedBy().getId().equals(principal.getId());
        if (!canUpdate) {
            throw new UnauthorizedException("Cannot update this task");
        }

        if (status != null) task.setStatus(status);
        if (completionNotes != null) task.setCompletionNotes(completionNotes);
        if ("COMPLETED".equals(status)) task.setCompletedAt(Instant.now());

        return careTaskRepository.save(task);
    }

    public List<MedicationSchedule> getAlertsForElderly(UUID elderlyUserId, UserPrincipal principal) {
        checkCaregiverAccess(elderlyUserId, principal);
        return scheduleRepository.findByPeriod(elderlyUserId,
                java.time.LocalDate.now().minusDays(1),
                java.time.LocalDate.now())
                .stream()
                .filter(s -> s.isEscalationSent() && !s.isEscalationAcknowledged())
                .collect(Collectors.toList());
    }

    @Transactional
    public MedicationSchedule acknowledgeAlert(UUID scheduleId, UserPrincipal principal) {
        MedicationSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", scheduleId));

        checkCaregiverAccess(schedule.getElderlyUser().getId(), principal);

        User acknowledgedBy = userRepository.findById(principal.getId()).orElseThrow();
        schedule.setEscalationAcknowledged(true);
        schedule.setEscalationAcknowledgedBy(acknowledgedBy);
        schedule.setEscalationAcknowledgedAt(Instant.now());

        return scheduleRepository.save(schedule);
    }

    private void checkCaregiverAccess(UUID elderlyUserId, UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        if (user.getRole() == UserRole.ELDERLY) {
            if (!user.getId().equals(elderlyUserId)) throw new UnauthorizedException("Access denied");
            return;
        }
        boolean hasAccess = relationshipRepository
                .existsByElderlyUserIdAndRelatedUserIdAndActiveTrue(elderlyUserId, principal.getId());
        if (!hasAccess && user.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("No authorized relationship");
        }
    }
}
