package com.eldercare.platform.dto.response;

import com.eldercare.platform.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
}
