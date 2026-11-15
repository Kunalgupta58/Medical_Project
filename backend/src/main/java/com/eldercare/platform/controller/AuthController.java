package com.eldercare.platform.controller;

import com.eldercare.platform.dto.request.LoginRequest;
import com.eldercare.platform.dto.request.RegisterRequest;
import com.eldercare.platform.dto.response.AuthResponse;
import com.eldercare.platform.dto.response.UserResponse;
import com.eldercare.platform.security.UserPrincipal;
import com.eldercare.platform.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, and profile endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Login and receive a JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/auth/logout")
    @Operation(summary = "Logout (client should discard token)")
    public ResponseEntity<Map<String, String>> logout() {
        // JWT is stateless; logout is client-side.
        return ResponseEntity.ok(Map.of("message", "Logged out successfully. Please discard your token."));
    }

    @GetMapping("/users/me")
    @Operation(summary = "Get current authenticated user")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal));
    }
}
