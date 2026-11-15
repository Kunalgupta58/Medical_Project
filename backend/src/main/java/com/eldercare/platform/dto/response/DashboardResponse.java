package com.eldercare.platform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardResponse {
    private long totalScheduledToday;
    private long takenToday;
    private long skippedToday;
    private long pendingToday;
    private long unconfirmedToday;
    private long upcomingAppointments;
    private long pendingCareTasks;
    private long activeMedications;
    private long totalDocuments;
    private long unreadNotifications;
    private double adherencePercentage;
    private List<Map<String, Object>> weeklyAdherence;
    private List<AppointmentResponse> nextAppointments;
    private List<NotificationResponse> recentNotifications;
}
