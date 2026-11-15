package com.eldercare.platform.repository;

import com.eldercare.platform.entity.CareTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CareTaskRepository extends JpaRepository<CareTask, UUID> {

    List<CareTask> findByElderlyUserIdOrderByCreatedAtDesc(UUID elderlyUserId);

    List<CareTask> findByAssignedToIdOrderByCreatedAtDesc(UUID assignedToUserId);

    @Query("SELECT t FROM CareTask t WHERE t.assignedTo.id = :userId AND t.status != 'COMPLETED' AND t.status != 'CANCELLED'")
    List<CareTask> findActiveTasks(UUID userId);

    long countByElderlyUserIdAndStatus(UUID elderlyUserId, String status);
}
