package com.eldercare.platform.controller;

import com.eldercare.platform.dto.request.MedicationRequest;
import com.eldercare.platform.dto.response.DoseResponse;
import com.eldercare.platform.dto.response.MedicationResponse;
import com.eldercare.platform.security.UserPrincipal;
import com.eldercare.platform.service.MedicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Medications", description = "Medication management and dose tracking")
public class MedicationController {

    private final MedicationService medicationService;

    @GetMapping("/medications")
    @Operation(summary = "List all medications")
    public ResponseEntity<List<MedicationResponse>> getAll(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(medicationService.getMedications(principal));
    }

    @PostMapping("/medications")
    @Operation(summary = "Create a medication")
    public ResponseEntity<MedicationResponse> create(@Valid @RequestBody MedicationRequest req,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicationService.createMedication(req, principal));
    }

    @GetMapping("/medications/{id}")
    @Operation(summary = "Get a medication by ID")
    public ResponseEntity<MedicationResponse> getOne(@PathVariable UUID id,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(medicationService.getMedication(id, principal));
    }

    @PutMapping("/medications/{id}")
    @Operation(summary = "Update a medication")
    public ResponseEntity<MedicationResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody MedicationRequest req,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(medicationService.updateMedication(id, req, principal));
    }

    @DeleteMapping("/medications/{id}")
    @Operation(summary = "Deactivate a medication (soft delete)")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        medicationService.deleteMedication(id, principal);
        return ResponseEntity.ok(Map.of("message", "Medication deactivated"));
    }

    // Dose schedule endpoints

    @GetMapping("/medication-schedules/today")
    @Operation(summary = "Get today's medication schedule")
    public ResponseEntity<List<DoseResponse>> getToday(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(medicationService.getTodaySchedule(principal));
    }

    @GetMapping("/medication-schedules/upcoming")
    @Operation(summary = "Get upcoming doses")
    public ResponseEntity<List<DoseResponse>> getUpcoming(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(medicationService.getUpcomingDoses(principal));
    }

    @PostMapping("/doses/{id}/taken")
    @Operation(summary = "Confirm a dose as taken")
    public ResponseEntity<DoseResponse> markTaken(@PathVariable UUID id,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(medicationService.markDoseTaken(id, principal));
    }

    @PostMapping("/doses/{id}/snooze")
    @Operation(summary = "Snooze a dose reminder by 15 minutes")
    public ResponseEntity<DoseResponse> snoozeDose(@PathVariable UUID id,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(medicationService.snoozeDose(id, principal));
    }

    @PostMapping("/doses/{id}/skip")
    @Operation(summary = "Skip a dose with an optional reason")
    public ResponseEntity<DoseResponse> skipDose(@PathVariable UUID id,
                                                  @RequestBody(required = false) Map<String, String> body,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(medicationService.skipDose(id, reason, principal));
    }
}
