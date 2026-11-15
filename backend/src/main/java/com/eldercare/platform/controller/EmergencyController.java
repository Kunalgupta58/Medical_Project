package com.eldercare.platform.controller;

import com.eldercare.platform.entity.EmergencyContact;
import com.eldercare.platform.security.UserPrincipal;
import com.eldercare.platform.service.EmergencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emergency")
@RequiredArgsConstructor
@Tag(name = "Emergency SOS", description = "Emergency SOS and contact management")
public class EmergencyController {

    private final EmergencyService emergencyService;

    @PostMapping("/sos")
    @Operation(summary = "Trigger emergency SOS")
    public ResponseEntity<Map<String, Object>> triggerSOS(
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        UUID elderlyUserId = body != null && body.get("elderlyUserId") != null
                ? UUID.fromString(body.get("elderlyUserId").toString()) : null;
        String notes = body != null ? (String) body.get("notes") : null;
        boolean isDemo = body != null && Boolean.TRUE.equals(body.get("isDemo"));

        return ResponseEntity.ok(emergencyService.triggerSOS(elderlyUserId, notes, principal, isDemo));
    }

    @GetMapping("/contacts")
    @Operation(summary = "Get emergency contacts")
    public ResponseEntity<List<EmergencyContact>> getContacts(
            @RequestParam(value = "elderlyUserId", required = false) UUID elderlyUserId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(emergencyService.getEmergencyContacts(elderlyUserId, principal));
    }

    @PostMapping("/contacts")
    @Operation(summary = "Add an emergency contact")
    public ResponseEntity<EmergencyContact> addContact(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        UUID elderlyUserId = body.get("elderlyUserId") != null
                ? UUID.fromString(body.get("elderlyUserId").toString()) : null;
        EmergencyContact contact = emergencyService.addEmergencyContact(
                elderlyUserId,
                (String) body.get("name"),
                (String) body.get("phone"),
                (String) body.get("email"),
                (String) body.get("relationship"),
                Boolean.TRUE.equals(body.get("isPrimary")),
                principal
        );
        return ResponseEntity.ok(contact);
    }
}
