package com.eldercare.platform.repository;

import com.eldercare.platform.entity.CaregiverRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaregiverRelationshipRepository extends JpaRepository<CaregiverRelationship, UUID> {

    List<CaregiverRelationship> findByElderlyUserIdAndActiveTrue(UUID elderlyUserId);

    List<CaregiverRelationship> findByRelatedUserIdAndActiveTrue(UUID relatedUserId);

    Optional<CaregiverRelationship> findByElderlyUserIdAndRelatedUserId(UUID elderlyUserId, UUID relatedUserId);

    boolean existsByElderlyUserIdAndRelatedUserIdAndActiveTrue(UUID elderlyUserId, UUID relatedUserId);

    @Query("SELECT r FROM CaregiverRelationship r WHERE r.elderlyUser.id = :elderlyUserId " +
           "AND r.active = true AND r.relationshipType = :type")
    List<CaregiverRelationship> findByElderlyUserAndType(UUID elderlyUserId, String type);
}
