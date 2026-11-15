package com.eldercare.platform.repository;

import com.eldercare.platform.entity.FamilyInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FamilyInvitationRepository extends JpaRepository<FamilyInvitation, UUID> {
    Optional<FamilyInvitation> findByInvitationToken(String token);
    List<FamilyInvitation> findByElderlyUserIdOrderByCreatedAtDesc(UUID elderlyUserId);
    List<FamilyInvitation> findByInviteeEmailAndStatus(String email, String status);
}
