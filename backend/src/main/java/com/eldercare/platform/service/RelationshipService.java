package com.eldercare.platform.service;

import com.eldercare.platform.entity.CaregiverRelationship;
import com.eldercare.platform.entity.FamilyInvitation;
import com.eldercare.platform.entity.User;
import com.eldercare.platform.dto.response.PendingInvitationResponse;
import com.eldercare.platform.enums.UserRole;
import com.eldercare.platform.enums.NotificationType;
import com.eldercare.platform.exception.BadRequestException;
import com.eldercare.platform.exception.ResourceNotFoundException;
import com.eldercare.platform.exception.UnauthorizedException;
import com.eldercare.platform.repository.CaregiverRelationshipRepository;
import com.eldercare.platform.repository.FamilyInvitationRepository;
import com.eldercare.platform.repository.UserRepository;
import com.eldercare.platform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RelationshipService {

    private final FamilyInvitationRepository invitationRepository;
    private final CaregiverRelationshipRepository relationshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public FamilyInvitation sendInvitation(String inviteeEmail, String inviteeRole,
                                            String message, UserPrincipal principal) {
        User elderlyUser = userRepository.findById(principal.getId()).orElseThrow();
        if (elderlyUser.getRole() != UserRole.ELDERLY) {
            throw new BadRequestException("Only elderly users can send invitations");
        }
        if (inviteeEmail == null || inviteeEmail.isBlank()) {
            throw new BadRequestException("Invitee email is required");
        }
        if (!"FAMILY_MEMBER".equalsIgnoreCase(inviteeRole) && !"CAREGIVER".equalsIgnoreCase(inviteeRole)) {
            throw new BadRequestException("Invite role must be FAMILY_MEMBER or CAREGIVER");
        }
        String normalizedEmail = inviteeEmail.trim().toLowerCase(Locale.ROOT);
        String normalizedRole = inviteeRole.toUpperCase(Locale.ROOT);

        // Check if the invitee is already connected
        userRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
            boolean alreadyLinked = relationshipRepository
                    .existsByElderlyUserIdAndRelatedUserIdAndActiveTrue(elderlyUser.getId(), existing.getId());
            if (alreadyLinked) {
                throw new BadRequestException("This user is already connected to your profile");
            }
        });

        String token = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        FamilyInvitation invitation = FamilyInvitation.builder()
                .elderlyUser(elderlyUser)
                .invitedBy(elderlyUser)
                .inviteeEmail(normalizedEmail)
                .inviteeRole(normalizedRole)
                .invitationToken(token)
                .status("PENDING")
                .permissions("{\"can_view_medications\":true,\"can_view_documents\":false}")
                .message(message)
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 3600)) // 7 days
                .build();

        FamilyInvitation saved = invitationRepository.save(invitation);

        // If the invitee already has an account, notify them
        userRepository.findByEmail(normalizedEmail).ifPresent(invitee -> {
            notificationService.createNotification(
                    invitee.getId(),
                    "Care Connection Invitation",
                    elderlyUser.getFullName() + " has invited you to connect as their "
                            + normalizedRole + ". Use token: " + token + " to accept.",
                    NotificationType.INVITATION,
                    "FamilyInvitation",
                    saved.getId()
            );
        });

        log.info("Invitation sent to {} by {}", inviteeEmail, elderlyUser.getId());
        return saved;
    }

    @Transactional
    public CaregiverRelationship acceptInvitation(String token, UserPrincipal principal) {
        FamilyInvitation invitation = invitationRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (!"PENDING".equals(invitation.getStatus())) {
            throw new BadRequestException("Invitation is no longer pending: " + invitation.getStatus());
        }
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus("EXPIRED");
            invitationRepository.save(invitation);
            throw new BadRequestException("Invitation has expired");
        }

        User acceptor = userRepository.findById(principal.getId()).orElseThrow();
        if (!acceptor.getEmail().equalsIgnoreCase(invitation.getInviteeEmail())) {
            throw new UnauthorizedException("This invitation is not for your email address");
        }
        if (!acceptor.getRole().name().equals(invitation.getInviteeRole())) {
            throw new BadRequestException("Register or sign in with the invited role to accept this invitation");
        }

        invitation.setStatus("ACCEPTED");
        invitationRepository.save(invitation);

        CaregiverRelationship relationship = CaregiverRelationship.builder()
                .elderlyUser(invitation.getElderlyUser())
                .relatedUser(acceptor)
                .relationshipType(invitation.getInviteeRole())
                .permissions(invitation.getPermissions())
                .active(true)
                .invitation(invitation)
                .build();

        CaregiverRelationship saved = relationshipRepository.save(relationship);

        // Notify the elderly user
        notificationService.createNotification(
                invitation.getElderlyUser().getId(),
                "Invitation Accepted",
                acceptor.getFullName() + " has accepted your invitation and is now connected as your "
                        + invitation.getInviteeRole() + ".",
                NotificationType.INVITATION,
                "CaregiverRelationship",
                saved.getId()
        );

        return saved;
    }

    public List<FamilyInvitation> getInvitations(UserPrincipal principal) {
        return invitationRepository.findByElderlyUserIdOrderByCreatedAtDesc(principal.getId());
    }

    @Transactional(readOnly = true)
    public List<PendingInvitationResponse> getPendingInvitations(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        if (user.getRole() != UserRole.FAMILY_MEMBER && user.getRole() != UserRole.CAREGIVER) {
            return List.of();
        }

        Instant now = Instant.now();
        return invitationRepository.findByInviteeEmailAndStatus(user.getEmail(), "PENDING")
                .stream()
                .filter(invitation -> invitation.getExpiresAt().isAfter(now))
                .map(invitation -> PendingInvitationResponse.builder()
                        .id(invitation.getId())
                        .elderlyUserName(invitation.getElderlyUser().getFullName())
                        .inviteeRole(invitation.getInviteeRole())
                        .message(invitation.getMessage())
                        .invitationToken(invitation.getInvitationToken())
                        .expiresAt(invitation.getExpiresAt())
                        .createdAt(invitation.getCreatedAt())
                        .build())
                .toList();
    }

    public List<CaregiverRelationship> getRelationships(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        if (user.getRole() == UserRole.ELDERLY) {
            return relationshipRepository.findByElderlyUserIdAndActiveTrue(user.getId());
        }
        return relationshipRepository.findByRelatedUserIdAndActiveTrue(user.getId());
    }

    @Transactional
    public void revokeRelationship(UUID relationshipId, UserPrincipal principal) {
        CaregiverRelationship rel = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Relationship", relationshipId));
        if (!rel.getElderlyUser().getId().equals(principal.getId())) {
            throw new UnauthorizedException("Only the elderly user can revoke access");
        }
        rel.setActive(false);
        relationshipRepository.save(rel);
    }
}
