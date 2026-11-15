package com.eldercare.platform.controller;

import com.eldercare.platform.dto.request.AppointmentRequest;
import com.eldercare.platform.dto.response.AppointmentResponse;
import com.eldercare.platform.security.UserPrincipal;
import com.eldercare.platform.service.AppointmentService;
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
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Appointment scheduling and management")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    @Operation(summary = "Get all appointments")
    public ResponseEntity<List<AppointmentResponse>> getAll(
            @RequestParam(value = "elderlyUserId", required = false) UUID elderlyUserId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(appointmentService.getAll(principal, elderlyUserId));
    }

    @PostMapping
    @Operation(summary = "Create an appointment")
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody AppointmentRequest req,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.create(req, principal));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an appointment")
    public ResponseEntity<AppointmentResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody AppointmentRequest req,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(appointmentService.update(id, req, principal));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an appointment")
    public ResponseEntity<AppointmentResponse> cancel(@PathVariable UUID id,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(appointmentService.cancel(id, principal));
    }
}
