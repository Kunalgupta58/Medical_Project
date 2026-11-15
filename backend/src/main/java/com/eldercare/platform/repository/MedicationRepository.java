package com.eldercare.platform.repository;

import com.eldercare.platform.entity.Medication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, UUID> {

    List<Medication> findByElderlyUserIdAndActiveTrue(UUID elderlyUserId);

    List<Medication> findByElderlyUserId(UUID elderlyUserId);

    Page<Medication> findByElderlyUserId(UUID elderlyUserId, Pageable pageable);

    @Query("SELECT m FROM Medication m WHERE m.elderlyUser.id = :elderlyUserId " +
           "AND m.active = true AND m.startDate <= :today " +
           "AND (m.endDate IS NULL OR m.endDate >= :today)")
    List<Medication> findActiveMedicationsForDate(UUID elderlyUserId, LocalDate today);

    long countByElderlyUserIdAndActiveTrue(UUID elderlyUserId);
}
