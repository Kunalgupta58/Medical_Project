package com.eldercare.platform.service;

import com.eldercare.platform.dto.response.PendingInvitationResponse;
import com.eldercare.platform.entity.FamilyInvitation;
import com.eldercare.platform.entity.User;
import com.eldercare.platform.enums.UserRole;
import com.eldercare.platform.repository.CaregiverRelationshipRepository;
import com.eldercare.platform.repository.FamilyInvitationRepository;
import com.eldercare.platform.repository.UserRepository;
import com.eldercare.platform.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelationshipServiceTest {

    @Mock
    private FamilyInvitationRepository invitationRepository;

    @Mock
    private CaregiverRelationshipRepository relationshipRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void getPendingInvitationsFindsInvitesForTheSignedInEmail() {
        UUID userId = UUID.randomUUID();
        User familyMember = User.builder()
                .id(userId)
                .email("family@example.com")
                .passwordHash("hashed")
                .firstName("Jamie")
                .lastName("Lee")
                .role(UserRole.FAMILY_MEMBER)
                .active(true)
                .build();
        User elderlyUser = User.builder()
                .id(UUID.randomUUID())
                .email("elder@example.com")
                .passwordHash("hashed")
                .firstName("Ava")
                .lastName("Patel")
                .role(UserRole.ELDERLY)
                .active(true)
                .build();
        FamilyInvitation invitation = FamilyInvitation.builder()
                .id(UUID.randomUUID())
                .elderlyUser(elderlyUser)
                .inviteeEmail(familyMember.getEmail())
                .inviteeRole("FAMILY_MEMBER")
                .invitationToken("invite-token")
                .message("Please join my care team")
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .status("PENDING")
                .build();

        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(familyMember));
        when(invitationRepository.findByInviteeEmailAndStatus(familyMember.getEmail(), "PENDING"))
                .thenReturn(List.of(invitation));

        RelationshipService relationshipService = new RelationshipService(
                invitationRepository, relationshipRepository, userRepository, null);
        List<PendingInvitationResponse> pending = relationshipService
                .getPendingInvitations(UserPrincipal.create(familyMember));

        assertEquals(1, pending.size());
        assertEquals("Ava Patel", pending.get(0).getElderlyUserName());
        assertEquals("invite-token", pending.get(0).getInvitationToken());
        verify(invitationRepository).findByInviteeEmailAndStatus(familyMember.getEmail(), "PENDING");
    }
}
