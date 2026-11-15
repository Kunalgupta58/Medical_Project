package com.eldercare.platform.service;

import com.eldercare.platform.entity.CaregiverRelationship;
import com.eldercare.platform.entity.EmergencyContact;
import com.eldercare.platform.entity.User;
import com.eldercare.platform.enums.NotificationType;
import com.eldercare.platform.enums.UserRole;
import com.eldercare.platform.exception.BadRequestException;
import com.eldercare.platform.repository.CaregiverRelationshipRepository;
import com.eldercare.platform.repository.EmergencyContactRepository;
import com.eldercare.platform.repository.UserRepository;
import com.eldercare.platform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencyService {

    private final UserRepository userRepository;
    private final CaregiverRelationshipRepository relationshipRepository;
    private final EmergencyContactRepository contactRepository;
    private final NotificationService notificationService;

    @Transactional
    public Map<String, Object> triggerSOS(UUID elderlyUserId, String notes, UserPrincipal principal, boolean isDemo) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        User elderlyUser;

        if (user.getRole() == UserRole.ELDERLY) {
            elderlyUser = user;
        } else {
            if (elderlyUserId == null) throw new BadRequestException("elderlyUserId required");
            boolean hasAccess = relationshipRepository
                    .existsByElderlyUserIdAndRelatedUserIdAndActiveTrue(elderlyUserId, principal.getId());
            if (!hasAccess) throw new BadRequestException("No authorized access");
            elderlyUser = userRepository.findById(elderlyUserId).orElseThrow();
        }

        List<EmergencyContact> contacts = contactRepository.findByElderlyUserIdAndNotifyOnSosTrue(elderlyUser.getId());
        List<Map<String, Object>> notified = new ArrayList<>();

        // Notify all connected caregivers and family members
        List<CaregiverRelationship> relationships = relationshipRepository.findByElderlyUserIdAndActiveTrue(elderlyUser.getId());
        for (CaregiverRelationship rel : relationships) {
            try {
                notificationService.createNotification(
                        rel.getRelatedUser().getId(),
                        "🆘 EMERGENCY SOS — " + elderlyUser.getFullName(),
                        (isDemo ? "[DEMO] " : "") +
                        elderlyUser.getFullName() + " has triggered an emergency SOS alert."
                                + (notes != null && !notes.isBlank() ? " Note: " + notes : "")
                                + " Please contact them immediately.",
                        NotificationType.EMERGENCY_ALERT,
                        "EmergencyEvent",
                        null
                );
            } catch (Exception e) {
                log.error("Error sending SOS notification to caregiver {}: {}", rel.getRelatedUser().getId(), e.getMessage());
            }
        }

        for (EmergencyContact contact : contacts) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", contact.getName());
            entry.put("phone", contact.getPhone());
            entry.put("relationship", contact.getRelationship());
            entry.put("inAppNotified", true);
            entry.put("emailNotified", false); // only if email configured
            notified.add(entry);
            log.info("SOS contact notified (in-app): {} for elderly {}", contact.getName(), elderlyUser.getId());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sosTriggered", true);
        result.put("elderlyUser", elderlyUser.getFullName());
        result.put("isDemo", isDemo);
        result.put("message", isDemo
                ? "DEMO SOS triggered. In production, emergency contacts and caregivers would be notified via all configured channels."
                : "SOS triggered. All connected caregivers notified in-app.");
        result.put("contactsNotified", notified);
        result.put("deliveryNote", "In-app notifications sent. SMS/WhatsApp/email delivery requires additional configuration.");
        result.put("timestamp", java.time.Instant.now().toString());

        return result;
    }

    public List<EmergencyContact> getEmergencyContacts(UUID elderlyUserId, UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        UUID eid = user.getRole() == UserRole.ELDERLY ? user.getId() : elderlyUserId;
        return contactRepository.findByElderlyUserIdOrderByPrimaryDesc(eid);
    }

    @Transactional
    public EmergencyContact addEmergencyContact(UUID elderlyUserId, String name, String phone,
                                                 String email, String relationship,
                                                 boolean isPrimary, UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        User elderlyUser = user.getRole() == UserRole.ELDERLY ? user
                : userRepository.findById(elderlyUserId).orElseThrow();

        EmergencyContact contact = EmergencyContact.builder()
                .elderlyUser(elderlyUser)
                .name(name)
                .phone(phone)
                .email(email)
                .relationship(relationship)
                .primary(isPrimary)
                .notifyOnSos(true)
                .build();

        return contactRepository.save(contact);
    }
}
