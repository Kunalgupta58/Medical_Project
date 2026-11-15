package com.eldercare.platform.service;

import com.eldercare.platform.dto.response.NotificationResponse;
import com.eldercare.platform.entity.Notification;
import com.eldercare.platform.entity.User;
import com.eldercare.platform.enums.NotificationType;
import com.eldercare.platform.exception.ResourceNotFoundException;
import com.eldercare.platform.repository.NotificationRepository;
import com.eldercare.platform.repository.UserRepository;
import com.eldercare.platform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Value("${application.email.enabled:false}")
    private boolean emailEnabled;

    public List<NotificationResponse> getNotifications(UserPrincipal principal, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(
                principal.getId(), PageRequest.of(page, size))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public long getUnreadCount(UserPrincipal principal) {
        return notificationRepository.countByUserIdAndReadFalse(principal.getId());
    }

    @Transactional
    public NotificationResponse markRead(UUID id, UserPrincipal principal) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        if (!n.getUser().getId().equals(principal.getId())) {
            throw new ResourceNotFoundException("Notification", id);
        }
        n.setRead(true);
        n.setReadAt(Instant.now());
        return toResponse(notificationRepository.save(n));
    }

    @Transactional
    public void markAllRead(UserPrincipal principal) {
        notificationRepository.markAllReadForUser(principal.getId());
    }

    @Transactional
    public Notification createNotification(UUID userId, String title, String message,
                                            NotificationType type, String entityType, UUID entityId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .notificationType(type)
                .relatedEntityType(entityType)
                .relatedEntityId(entityId)
                .read(false)
                .emailSent(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        if (emailEnabled) {
            tryEmailNotification(saved);
        } else {
            log.debug("Email disabled. Notification stored in-app: {}", title);
        }

        return saved;
    }

    private void tryEmailNotification(Notification notification) {
        // Email is only attempted when EMAIL_ENABLED=true and SMTP credentials are configured.
        // If unconfigured, the notification is stored in-app and delivery status reflects that.
        log.info("Email notification attempt for user {} — delivery not configured", notification.getUser().getId());
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .notificationType(n.getNotificationType())
                .relatedEntityType(n.getRelatedEntityType())
                .relatedEntityId(n.getRelatedEntityId())
                .read(n.isRead())
                .readAt(n.getReadAt())
                .emailSent(n.isEmailSent())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
