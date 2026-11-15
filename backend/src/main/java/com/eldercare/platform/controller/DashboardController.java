package com.eldercare.platform.controller;

import com.eldercare.platform.dto.response.DashboardResponse;
import com.eldercare.platform.security.UserPrincipal;
import com.eldercare.platform.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard statistics and overview")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @Operation(summary = "Get dashboard overview statistics")
    public ResponseEntity<DashboardResponse> getOverview(
            @RequestParam(value = "elderlyUserId", required = false) UUID elderlyUserId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(dashboardService.getOverview(principal, elderlyUserId));
    }
}
