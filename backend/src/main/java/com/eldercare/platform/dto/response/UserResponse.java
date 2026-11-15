package com.eldercare.platform.dto.response;

import com.eldercare.platform.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private UserRole role;
    private boolean active;
    private String profileImageUrl;
    private Instant createdAt;
}
