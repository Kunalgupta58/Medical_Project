package com.eldercare.platform.controller;

import com.eldercare.platform.entity.CaregiverRelationship;
import com.eldercare.platform.entity.FamilyInvitation;
import com.eldercare.platform.dto.response.PendingInvitationResponse;
import com.eldercare.platform.security.UserPrincipal;
import com.eldercare.platform.service.RelationshipService;
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
@RequestMapping("/api/v1/relationships")
@RequiredArgsConstructor
@Tag(name = "Relationships", description = "Family and caregiver connection management")
public class RelationshipController {

    private final RelationshipService relationshipService;

    @PostMapping("/invite")
    @Operation(summary = "Send a care connection invitation")
    public ResponseEntity<FamilyInvitation> invite(@RequestBody Map<String, String> body,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        FamilyInvitation invitation = relationshipService.sendInvitation(
                body.get("email"),
                body.get("role"),
                body.get("message"),
                principal
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(invitation);
    }

    @PostMapping("/accept")
    @Operation(summary = "Accept an invitation using the token")
    public ResponseEntity<CaregiverRelationship> accept(@RequestBody Map<String, String> body,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(relationshipService.acceptInvitation(body.get("token"), principal));
    }

    @GetMapping("/invitations")
    @Operation(summary = "List invitations sent by the current elderly user")
    public ResponseEntity<List<FamilyInvitation>> getInvitations(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(relationshipService.getInvitations(principal));
    }

    @GetMapping("/pending")
    @Operation(summary = "List pending invitations addressed to the current user's email")
    public ResponseEntity<List<PendingInvitationResponse>> getPendingInvitations(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(relationshipService.getPendingInvitations(principal));
    }

    @GetMapping
    @Operation(summary = "List active relationships")
    public ResponseEntity<List<CaregiverRelationship>> getRelationships(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(relationshipService.getRelationships(principal));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Revoke a care relationship")
    public ResponseEntity<Map<String, String>> revoke(@PathVariable UUID id,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        relationshipService.revokeRelationship(id, principal);
        return ResponseEntity.ok(Map.of("message", "Relationship revoked"));
    }
}
