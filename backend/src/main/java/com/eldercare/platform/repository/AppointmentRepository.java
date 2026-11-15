package com.eldercare.platform.repository;

import com.eldercare.platform.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Query("SELECT a FROM Appointment a WHERE a.elderlyUser.id = :elderlyUserId " +
           "AND a.appointmentDatetime >= :now AND a.status = 'SCHEDULED' " +
           "ORDER BY a.appointmentDatetime ASC")
    List<Appointment> findUpcoming(UUID elderlyUserId, Instant now);

    @Query("SELECT a FROM Appointment a WHERE a.elderlyUser.id = :elderlyUserId " +
           "AND a.appointmentDatetime < :now ORDER BY a.appointmentDatetime DESC")
    List<Appointment> findPast(UUID elderlyUserId, Instant now);

    @Query("SELECT a FROM Appointment a WHERE a.elderlyUser.id = :elderlyUserId " +
           "ORDER BY a.appointmentDatetime DESC")
    List<Appointment> findAll(UUID elderlyUserId);

    @Query("SELECT a FROM Appointment a WHERE a.status = 'SCHEDULED' " +
           "AND a.appointmentDatetime BETWEEN :from AND :to AND a.reminderSent = false")
    List<Appointment> findForReminder(Instant from, Instant to);

    long countByElderlyUserIdAndStatus(UUID elderlyUserId, String status);
}
