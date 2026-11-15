package com.eldercare.platform.repository;

import com.eldercare.platform.entity.MedicationSchedule;
import com.eldercare.platform.enums.DoseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, UUID> {

    List<MedicationSchedule> findByElderlyUserIdAndScheduledDate(UUID elderlyUserId, LocalDate date);

    @Query("SELECT s FROM MedicationSchedule s WHERE s.elderlyUser.id = :elderlyUserId " +
           "AND s.scheduledDate = :date ORDER BY s.scheduledTime ASC")
    List<MedicationSchedule> findTodaySchedule(UUID elderlyUserId, LocalDate date);

    @Query("SELECT s FROM MedicationSchedule s WHERE s.elderlyUser.id = :elderlyUserId " +
           "AND s.scheduledDatetime > :now AND s.status = 'PENDING' " +
           "ORDER BY s.scheduledDatetime ASC")
    List<MedicationSchedule> findUpcoming(UUID elderlyUserId, Instant now);

    @Query("SELECT s FROM MedicationSchedule s WHERE s.medication.id = :medicationId " +
           "AND s.scheduledDatetime = :dt")
    Optional<MedicationSchedule> findByMedicationIdAndScheduledDatetime(UUID medicationId, Instant dt);

    @Query("SELECT s FROM MedicationSchedule s WHERE s.status = 'PENDING' " +
           "AND s.scheduledDatetime < :threshold AND s.escalationSent = false")
    List<MedicationSchedule> findUnconfirmedForEscalation(Instant threshold);

    @Query("SELECT COUNT(s) FROM MedicationSchedule s WHERE s.elderlyUser.id = :elderlyUserId " +
           "AND s.scheduledDate BETWEEN :from AND :to")
    long countScheduledInPeriod(UUID elderlyUserId, LocalDate from, LocalDate to);

    @Query("SELECT COUNT(s) FROM MedicationSchedule s WHERE s.elderlyUser.id = :elderlyUserId " +
           "AND s.scheduledDate BETWEEN :from AND :to AND s.status = 'TAKEN'")
    long countTakenInPeriod(UUID elderlyUserId, LocalDate from, LocalDate to);

    @Query("SELECT s FROM MedicationSchedule s WHERE s.elderlyUser.id = :elderlyUserId " +
           "AND s.scheduledDate BETWEEN :from AND :to ORDER BY s.scheduledDatetime DESC")
    List<MedicationSchedule> findByPeriod(UUID elderlyUserId, LocalDate from, LocalDate to);

    List<MedicationSchedule> findByMedicationId(UUID medicationId);
}
