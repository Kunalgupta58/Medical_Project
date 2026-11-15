package com.eldercare.platform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "family_invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elderly_user_id", nullable = false)
    private User elderlyUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_user_id", nullable = false)
    private User invitedBy;

    @Column(name = "invitee_email", nullable = false)
    private String inviteeEmail;

    @Column(name = "invitee_role", nullable = false)
    private String inviteeRole;

    @Column(name = "invitation_token", nullable = false, unique = true)
    private String invitationToken;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private String permissions = "{}";

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
