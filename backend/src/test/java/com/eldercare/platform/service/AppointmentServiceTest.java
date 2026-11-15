package com.eldercare.platform.service;

import com.eldercare.platform.dto.response.AppointmentResponse;
import com.eldercare.platform.entity.Appointment;
import com.eldercare.platform.entity.CaregiverRelationship;
import com.eldercare.platform.entity.User;
import com.eldercare.platform.enums.UserRole;
import com.eldercare.platform.exception.BadRequestException;
import com.eldercare.platform.repository.AppointmentRepository;
import com.eldercare.platform.repository.CaregiverRelationshipRepository;
import com.eldercare.platform.repository.UserRepository;
import com.eldercare.platform.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaregiverRelationshipRepository relationshipRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void getAll_usesSingleConnectedElderlyProfileWhenNoExplicitIdIsProvided() {
        UUID principalId = UUID.randomUUID();
        UUID elderlyId = UUID.randomUUID();

        User familyMember = User.builder()
                .id(principalId)
                .email("family@example.com")
                .passwordHash("hashed")
                .firstName("Jamie")
                .lastName("Lee")
                .role(UserRole.FAMILY_MEMBER)
                .active(true)
                .build();

        User elderlyUser = User.builder()
                .id(elderlyId)
                .email("elder@example.com")
                .passwordHash("hashed")
                .firstName("Ava")
                .lastName("Patel")
                .role(UserRole.ELDERLY)
                .active(true)
                .build();

        CaregiverRelationship relationship = CaregiverRelationship.builder()
                .elderlyUser(elderlyUser)
                .relatedUser(familyMember)
                .relationshipType("FAMILY_MEMBER")
                .active(true)
                .build();

        Appointment appt = Appointment.builder()
                .id(UUID.randomUUID())
                .elderlyUser(elderlyUser)
                .doctorName("Dr. Patel")
                .hospitalName("City Clinic")
                .appointmentDatetime(Instant.now().plusSeconds(3600))
                .purpose("Annual checkup")
                .status("SCHEDULED")
                .build();

        when(userRepository.findById(principalId)).thenReturn(Optional.of(familyMember));
        when(relationshipRepository.findByRelatedUserIdAndActiveTrue(principalId)).thenReturn(List.of(relationship));
        when(appointmentRepository.findAll(elderlyId)).thenReturn(List.of(appt));

        List<AppointmentResponse> response = appointmentService.getAll(UserPrincipal.create(familyMember), null);

        assertEquals(1, response.size());
        assertEquals(elderlyId, response.get(0).getElderlyUserId());
        assertEquals("Dr. Patel", response.get(0).getDoctorName());
    }

    @Test
    void getAll_rejectsMultipleConnectedProfilesWithoutExplicitElderlyUserId() {
        UUID principalId = UUID.randomUUID();
        User familyMember = User.builder()
                .id(principalId)
                .email("family@example.com")
                .passwordHash("hashed")
                .firstName("Jamie")
                .lastName("Lee")
                .role(UserRole.FAMILY_MEMBER)
                .active(true)
                .build();

        User elderlyOne = User.builder().id(UUID.randomUUID()).firstName("Ava").lastName("Patel").role(UserRole.ELDERLY).build();
        User elderlyTwo = User.builder().id(UUID.randomUUID()).firstName("Noah").lastName("Singh").role(UserRole.ELDERLY).build();

        when(userRepository.findById(principalId)).thenReturn(Optional.of(familyMember));
        when(relationshipRepository.findByRelatedUserIdAndActiveTrue(principalId)).thenReturn(List.of(
                CaregiverRelationship.builder().elderlyUser(elderlyOne).relatedUser(familyMember).relationshipType("FAMILY_MEMBER").active(true).build(),
                CaregiverRelationship.builder().elderlyUser(elderlyTwo).relatedUser(familyMember).relationshipType("CAREGIVER").active(true).build()
        ));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> appointmentService.getAll(UserPrincipal.create(familyMember), null));

        assertTrue(ex.getMessage().contains("Multiple elderly profiles"));
    }
}
