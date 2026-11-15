package com.eldercare.platform.service;

import com.eldercare.platform.dto.request.MedicationRequest;
import com.eldercare.platform.dto.response.DoseResponse;
import com.eldercare.platform.dto.response.MedicationResponse;
import com.eldercare.platform.entity.Medication;
import com.eldercare.platform.entity.MedicationSchedule;
import com.eldercare.platform.entity.User;
import com.eldercare.platform.enums.DoseStatus;
import com.eldercare.platform.enums.UserRole;
import com.eldercare.platform.exception.BadRequestException;
import com.eldercare.platform.exception.ResourceNotFoundException;
import com.eldercare.platform.exception.UnauthorizedException;
import com.eldercare.platform.repository.CaregiverRelationshipRepository;
import com.eldercare.platform.repository.MedicationRepository;
import com.eldercare.platform.repository.MedicationScheduleRepository;
import com.eldercare.platform.repository.UserRepository;
import com.eldercare.platform.security.UserPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final MedicationScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final CaregiverRelationshipRepository relationshipRepository;
    private final ObjectMapper objectMapper;

    public List<MedicationResponse> getMedications(UserPrincipal principal) {
        UUID elderlyUserId = resolveElderlyUserId(principal, null);
        return medicationRepository.findByElderlyUserId(elderlyUserId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public MedicationResponse createMedication(MedicationRequest req, UserPrincipal principal) {
        User elderlyUser = resolveElderlyUser(principal, req.getElderlyUserId());
        User creator = userRepository.findById(principal.getId()).orElseThrow();

        String timesJson = "[]";
        if (req.getScheduledTimes() != null && !req.getScheduledTimes().isEmpty()) {
            try {
                timesJson = objectMapper.writeValueAsString(req.getScheduledTimes());
            } catch (JsonProcessingException e) {
                throw new BadRequestException("Invalid scheduled times format");
            }
        }

        Medication medication = Medication.builder()
                .elderlyUser(elderlyUser)
                .createdBy(creator)
                .name(req.getName())
                .genericName(req.getGenericName())
                .dosageText(req.getDosageText())
                .medicineType(req.getMedicineType())
                .frequency(req.getFrequency())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .scheduledTimesJson(timesJson)
                .instructions(req.getInstructions())
                .prescribingDoctor(req.getPrescribingDoctor())
                .notes(req.getNotes())
                .color(req.getColor() != null ? req.getColor() : "#3B82F6")
                .active(true)
                .build();

        medicationRepository.save(medication);

        // Generate schedules for the next 7 days
        generateSchedulesForMedication(medication, LocalDate.now(), LocalDate.now().plusDays(7));

        return toResponse(medication);
    }

    public MedicationResponse getMedication(UUID id, UserPrincipal principal) {
        Medication med = medicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medication", id));
        checkAccess(med.getElderlyUser().getId(), principal);
        return toResponse(med);
    }

    @Transactional
    public MedicationResponse updateMedication(UUID id, MedicationRequest req, UserPrincipal principal) {
        Medication med = medicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medication", id));
        checkAccess(med.getElderlyUser().getId(), principal);

        med.setName(req.getName());
        med.setGenericName(req.getGenericName());
        med.setDosageText(req.getDosageText());
        med.setMedicineType(req.getMedicineType());
        med.setFrequency(req.getFrequency());
        med.setStartDate(req.getStartDate());
        med.setEndDate(req.getEndDate());
        if (req.getInstructions() != null) med.setInstructions(req.getInstructions());
        if (req.getPrescribingDoctor() != null) med.setPrescribingDoctor(req.getPrescribingDoctor());
        if (req.getNotes() != null) med.setNotes(req.getNotes());
        if (req.getColor() != null) med.setColor(req.getColor());

        if (req.getScheduledTimes() != null) {
            try {
                med.setScheduledTimesJson(objectMapper.writeValueAsString(req.getScheduledTimes()));
            } catch (JsonProcessingException e) {
                throw new BadRequestException("Invalid times");
            }
        }

        medicationRepository.save(med);
        return toResponse(med);
    }

    @Transactional
    public void deleteMedication(UUID id, UserPrincipal principal) {
        Medication med = medicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medication", id));
        checkAccess(med.getElderlyUser().getId(), principal);
        med.setActive(false);
        medicationRepository.save(med);
    }

    public List<DoseResponse> getTodaySchedule(UserPrincipal principal) {
        UUID elderlyUserId = resolveElderlyUserId(principal, null);
        List<MedicationSchedule> schedules = scheduleRepository.findTodaySchedule(elderlyUserId, LocalDate.now());
        return schedules.stream().map(this::toDoseResponse).collect(Collectors.toList());
    }

    public List<DoseResponse> getUpcomingDoses(UserPrincipal principal) {
        UUID elderlyUserId = resolveElderlyUserId(principal, null);
        List<MedicationSchedule> upcoming = scheduleRepository.findUpcoming(elderlyUserId, Instant.now());
        return upcoming.stream().limit(20).map(this::toDoseResponse).collect(Collectors.toList());
    }

    @Transactional
    public DoseResponse markDoseTaken(UUID doseId, UserPrincipal principal) {
        MedicationSchedule schedule = scheduleRepository.findById(doseId)
                .orElseThrow(() -> new ResourceNotFoundException("Dose", doseId));
        checkAccess(schedule.getElderlyUser().getId(), principal);

        if (schedule.getStatus() == DoseStatus.TAKEN) {
            throw new BadRequestException("This dose has already been marked as taken");
        }

        User confirmedBy = userRepository.findById(principal.getId()).orElseThrow();
        schedule.setStatus(DoseStatus.TAKEN);
        schedule.setTakenAt(Instant.now());
        schedule.setConfirmedBy(confirmedBy);

        return toDoseResponse(scheduleRepository.save(schedule));
    }

    @Transactional
    public DoseResponse snoozeDose(UUID doseId, UserPrincipal principal) {
        MedicationSchedule schedule = scheduleRepository.findById(doseId)
                .orElseThrow(() -> new ResourceNotFoundException("Dose", doseId));
        checkAccess(schedule.getElderlyUser().getId(), principal);

        if (schedule.getSnoozeCount() >= 3) {
            throw new BadRequestException("Maximum snooze limit (3) reached for this dose");
        }

        schedule.setStatus(DoseStatus.SNOOZED);
        schedule.setSnoozeCount(schedule.getSnoozeCount() + 1);
        schedule.setSnoozedUntil(Instant.now().plusSeconds(15 * 60)); // 15 minutes

        return toDoseResponse(scheduleRepository.save(schedule));
    }

    @Transactional
    public DoseResponse skipDose(UUID doseId, String reason, UserPrincipal principal) {
        MedicationSchedule schedule = scheduleRepository.findById(doseId)
                .orElseThrow(() -> new ResourceNotFoundException("Dose", doseId));
        checkAccess(schedule.getElderlyUser().getId(), principal);

        schedule.setStatus(DoseStatus.SKIPPED);
        schedule.setSkipReason(reason);

        return toDoseResponse(scheduleRepository.save(schedule));
    }

    @Transactional
    public void generateSchedulesForMedication(Medication medication, LocalDate from, LocalDate to) {
        if (!medication.isActive()) return;

        List<LocalTime> times = parseScheduledTimes(medication.getScheduledTimesJson());
        if (times.isEmpty()) return;

        LocalDate start = from.isBefore(medication.getStartDate()) ? medication.getStartDate() : from;
        LocalDate end = medication.getEndDate() != null && medication.getEndDate().isBefore(to)
                ? medication.getEndDate() : to;

        ZoneId zone = ZoneId.systemDefault();
        List<MedicationSchedule> toCreate = new ArrayList<>();

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            for (LocalTime time : times) {
                Instant scheduledInstant = date.atTime(time).atZone(zone).toInstant();

                // Idempotent: skip if already exists
                boolean exists = scheduleRepository
                        .findByMedicationIdAndScheduledDatetime(medication.getId(), scheduledInstant)
                        .isPresent();

                if (!exists) {
                    final LocalDate finalDate = date;
                    toCreate.add(MedicationSchedule.builder()
                            .medication(medication)
                            .elderlyUser(medication.getElderlyUser())
                            .scheduledDate(finalDate)
                            .scheduledTime(time)
                            .scheduledDatetime(scheduledInstant)
                            .status(DoseStatus.PENDING)
                            .build());
                }
            }
        }

        if (!toCreate.isEmpty()) {
            scheduleRepository.saveAll(toCreate);
            log.info("Generated {} schedules for medication {}", toCreate.size(), medication.getId());
        }
    }

    private List<LocalTime> parseScheduledTimes(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return List.of();
        try {
            List<String> strings = objectMapper.readValue(json, new TypeReference<>() {});
            return strings.stream()
                    .map(s -> LocalTime.parse(s.trim()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Could not parse scheduled times: {}", json);
            return List.of();
        }
    }

    private UUID resolveElderlyUserId(UserPrincipal principal, UUID requestedId) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        if (user.getRole() == UserRole.ELDERLY) {
            return user.getId();
        }
        if (requestedId != null) {
            checkAccess(requestedId, principal);
            return requestedId;
        }
        if (user.getRole() == UserRole.ADMIN) {
            throw new BadRequestException("elderlyUserId is required for admin access");
        }

        List<com.eldercare.platform.entity.CaregiverRelationship> relationships = relationshipRepository
                .findByRelatedUserIdAndActiveTrue(principal.getId());

        if (relationships.size() == 1) {
            return relationships.get(0).getElderlyUser().getId();
        }
        if (relationships.size() > 1) {
            throw new BadRequestException("Multiple elderly profiles are connected; please specify elderlyUserId");
        }
        throw new BadRequestException("elderlyUserId is required for this role");
    }

    private User resolveElderlyUser(UserPrincipal principal, UUID requestedId) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        if (user.getRole() == UserRole.ELDERLY) {
            return user;
        }
        if (requestedId != null) {
            User elderlyUser = userRepository.findById(requestedId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", requestedId));
            checkAccess(requestedId, principal);
            return elderlyUser;
        }
        UUID fallbackId = resolveElderlyUserId(principal, null);
        return userRepository.findById(fallbackId)
                .orElseThrow(() -> new ResourceNotFoundException("User", fallbackId));
    }

    private void checkAccess(UUID elderlyUserId, UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        if (user.getRole() == UserRole.ELDERLY) {
            if (!user.getId().equals(elderlyUserId)) {
                throw new UnauthorizedException("Access denied to this profile");
            }
        } else if (user.getRole() == UserRole.ADMIN) {
            // Admin can read but not modify patient data
        } else {
            boolean hasAccess = relationshipRepository
                    .existsByElderlyUserIdAndRelatedUserIdAndActiveTrue(elderlyUserId, principal.getId());
            if (!hasAccess) {
                throw new UnauthorizedException("No authorized relationship with this profile");
            }
        }
    }

    private MedicationResponse toResponse(Medication m) {
        List<String> times = parseScheduledTimes(m.getScheduledTimesJson())
                .stream().map(LocalTime::toString).collect(Collectors.toList());
        return MedicationResponse.builder()
                .id(m.getId())
                .elderlyUserId(m.getElderlyUser().getId())
                .elderlyUserName(m.getElderlyUser().getFullName())
                .name(m.getName())
                .genericName(m.getGenericName())
                .dosageText(m.getDosageText())
                .medicineType(m.getMedicineType())
                .frequency(m.getFrequency())
                .startDate(m.getStartDate())
                .endDate(m.getEndDate())
                .scheduledTimes(times)
                .instructions(m.getInstructions())
                .prescribingDoctor(m.getPrescribingDoctor())
                .notes(m.getNotes())
                .active(m.isActive())
                .color(m.getColor())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    private DoseResponse toDoseResponse(MedicationSchedule s) {
        Medication med = s.getMedication();
        return DoseResponse.builder()
                .id(s.getId())
                .medicationId(med.getId())
                .medicationName(med.getName())
                .dosageText(med.getDosageText())
                .medicineType(med.getMedicineType().name())
                .color(med.getColor())
                .instructions(med.getInstructions())
                .scheduledDate(s.getScheduledDate())
                .scheduledTime(s.getScheduledTime())
                .scheduledDatetime(s.getScheduledDatetime())
                .status(s.getStatus())
                .takenAt(s.getTakenAt())
                .confirmedByName(s.getConfirmedBy() != null ? s.getConfirmedBy().getFullName() : null)
                .snoozeCount(s.getSnoozeCount())
                .snoozedUntil(s.getSnoozedUntil())
                .skipReason(s.getSkipReason())
                .escalationSent(s.isEscalationSent())
                .escalationAcknowledged(s.isEscalationAcknowledged())
                .build();
    }
}
