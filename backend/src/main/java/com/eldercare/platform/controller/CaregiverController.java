package com.eldercare.platform.controller;

import com.eldercare.platform.entity.CareTask;
import com.eldercare.platform.entity.MedicationSchedule;
import com.eldercare.platform.security.UserPrincipal;
import com.eldercare.platform.service.CaregiverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/caregiver")
@RequiredArgsConstructor
@Tag(name = "Caregiver", description = "Caregiver task and alert management")
public class CaregiverController {

    private final CaregiverService caregiverService;

    @GetMapping("/tasks")
    @Operation(summary = "Get tasks assigned to me")
    public ResponseEntity<List<CareTask>> getMyTasks(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(caregiverService.getMyTasks(principal));
    }

    @GetMapping("/tasks/elderly/{elderlyUserId}")
    @Operation(summary = "Get tasks for an elderly user")
    public ResponseEntity<List<CareTask>> getTasksForElderly(@PathVariable UUID elderlyUserId,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(caregiverService.getTasksForElderly(elderlyUserId, principal));
    }

    @PostMapping("/tasks")
    @Operation(summary = "Create a care task")
    public ResponseEntity<CareTask> createTask(@RequestBody Map<String, Object> body,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        UUID elderlyUserId = UUID.fromString(body.get("elderlyUserId").toString());
        UUID assignedToId = body.get("assignedToUserId") != null
                ? UUID.fromString(body.get("assignedToUserId").toString()) : null;

        CareTask task = caregiverService.createTask(
                elderlyUserId,
                (String) body.get("title"),
                (String) body.get("description"),
                (String) body.get("taskType"),
                (String) body.get("priority"),
                (String) body.get("dueDate"),
                assignedToId,
                principal
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @PutMapping("/tasks/{id}")
    @Operation(summary = "Update a care task status")
    public ResponseEntity<CareTask> updateTask(@PathVariable UUID id,
                                                @RequestBody Map<String, String> body,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(caregiverService.updateTask(id, body.get("status"), body.get("completionNotes"), principal));
    }

    @GetMapping("/alerts/{elderlyUserId}")
    @Operation(summary = "Get escalation alerts for an elderly user")
    public ResponseEntity<List<MedicationSchedule>> getAlerts(@PathVariable UUID elderlyUserId,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(caregiverService.getAlertsForElderly(elderlyUserId, principal));
    }

    @PostMapping("/alerts/{scheduleId}/acknowledge")
    @Operation(summary = "Acknowledge a medication escalation alert")
    public ResponseEntity<MedicationSchedule> acknowledgeAlert(@PathVariable UUID scheduleId,
                                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(caregiverService.acknowledgeAlert(scheduleId, principal));
    }
}
