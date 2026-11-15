package com.eldercare.platform.service;

import com.eldercare.platform.dto.request.AppointmentRequest;
import com.eldercare.platform.dto.response.AppointmentResponse;
import com.eldercare.platform.entity.Appointment;
import com.eldercare.platform.entity.CaregiverRelationship;
import com.eldercare.platform.entity.User;
import com.eldercare.platform.enums.UserRole;
import com.eldercare.platform.exception.BadRequestException;
import com.eldercare.platform.exception.ResourceNotFoundException;
import com.eldercare.platform.exception.UnauthorizedException;
import com.eldercare.platform.repository.AppointmentRepository;
import com.eldercare.platform.repository.CaregiverRelationshipRepository;
import com.eldercare.platform.repository.UserRepository;
import com.eldercare.platform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final CaregiverRelationshipRepository relationshipRepository;

    public List<AppointmentResponse> getUpcoming(UserPrincipal principal, UUID elderlyUserId) {
        UUID eid = resolveElderlyUserId(principal, elderlyUserId);
        return appointmentRepository.findUpcoming(eid, Instant.now())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<AppointmentResponse> getPast(UserPrincipal principal, UUID elderlyUserId) {
        UUID eid = resolveElderlyUserId(principal, elderlyUserId);
        return appointmentRepository.findPast(eid, Instant.now())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<AppointmentResponse> getAll(UserPrincipal principal, UUID elderlyUserId) {
        UUID eid = resolveElderlyUserId(principal, elderlyUserId);
        return appointmentRepository.findAll(eid)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public AppointmentResponse create(AppointmentRequest req, UserPrincipal principal) {
        User creator = userRepository.findById(principal.getId()).orElseThrow();
        User elderlyUser = resolveElderlyUser(principal, req.getElderlyUserId());

        Appointment appt = Appointment.builder()
                .elderlyUser(elderlyUser)
                .createdBy(creator)
                .doctorName(req.getDoctorName())
                .hospitalName(req.getHospitalName())
                .appointmentDatetime(req.getAppointmentDatetime())
                .purpose(req.getPurpose())
                .notes(req.getNotes())
                .status("SCHEDULED")
                .build();

        return toResponse(appointmentRepository.save(appt));
    }

    @Transactional
    public AppointmentResponse update(UUID id, AppointmentRequest req, UserPrincipal principal) {
        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
        checkAccess(appt.getElderlyUser().getId(), principal);

        appt.setDoctorName(req.getDoctorName());
        if (req.getHospitalName() != null) appt.setHospitalName(req.getHospitalName());
        appt.setAppointmentDatetime(req.getAppointmentDatetime());
        appt.setPurpose(req.getPurpose());
        if (req.getNotes() != null) appt.setNotes(req.getNotes());

        return toResponse(appointmentRepository.save(appt));
    }

    @Transactional
    public AppointmentResponse cancel(UUID id, UserPrincipal principal) {
        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
        checkAccess(appt.getElderlyUser().getId(), principal);

        if ("CANCELLED".equals(appt.getStatus())) {
            throw new BadRequestException("Appointment is already cancelled");
        }
        appt.setStatus("CANCELLED");
        return toResponse(appointmentRepository.save(appt));
    }

    private UUID resolveElderlyUserId(UserPrincipal principal, UUID requestedId) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        if (user.getRole() == UserRole.ELDERLY) return user.getId();
        if (requestedId != null) {
            checkAccess(requestedId, principal);
            return requestedId;
        }
        return resolveDefaultElderlyUserId(user, principal);
    }

    private User resolveElderlyUser(UserPrincipal principal, UUID requestedId) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        if (user.getRole() == UserRole.ELDERLY) return user;
        if (requestedId == null) {
            UUID fallbackId = resolveDefaultElderlyUserId(user, principal);
            return userRepository.findById(fallbackId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", fallbackId));
        }
        checkAccess(requestedId, principal);
        return userRepository.findById(requestedId)
                .orElseThrow(() -> new ResourceNotFoundException("User", requestedId));
    }

    private UUID resolveDefaultElderlyUserId(User user, UserPrincipal principal) {
        if (user.getRole() == UserRole.ADMIN) {
            throw new BadRequestException("elderlyUserId is required for admin access");
        }

        List<CaregiverRelationship> relationships = relationshipRepository
                .findByRelatedUserIdAndActiveTrue(principal.getId());

        if (relationships.size() == 1) {
            return relationships.get(0).getElderlyUser().getId();
        }
        if (relationships.size() > 1) {
            throw new BadRequestException("Multiple elderly profiles are connected; please specify elderlyUserId");
        }
        throw new BadRequestException("elderlyUserId is required");
    }

    private void checkAccess(UUID elderlyUserId, UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        if (user.getRole() == UserRole.ELDERLY && !user.getId().equals(elderlyUserId)) {
            throw new UnauthorizedException("Access denied");
        }
        if (user.getRole() != UserRole.ELDERLY && user.getRole() != UserRole.ADMIN) {
            boolean hasAccess = relationshipRepository
                    .existsByElderlyUserIdAndRelatedUserIdAndActiveTrue(elderlyUserId, principal.getId());
            if (!hasAccess) throw new UnauthorizedException("No authorized relationship");
        }
    }

    private AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .elderlyUserId(a.getElderlyUser().getId())
                .elderlyUserName(a.getElderlyUser().getFullName())
                .doctorName(a.getDoctorName())
                .hospitalName(a.getHospitalName())
                .appointmentDatetime(a.getAppointmentDatetime())
                .purpose(a.getPurpose())
                .notes(a.getNotes())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
