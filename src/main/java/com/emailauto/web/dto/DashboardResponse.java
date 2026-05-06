package com.emailauto.web.dto;

import java.time.Instant;
import java.util.List;

public record DashboardResponse(boolean authenticated, String email, long totalSent, long totalFailed,
                                List<CampaignSummary> recentCampaigns, List<FailureSummary> recentFailures) {
    public record CampaignSummary(Long id, String subject, String status, Instant scheduledAt, int sentCount, int failedCount, Instant createdAt) {
    }

    public record FailureSummary(String recipientEmail, String errorMessage, Instant createdAt) {
    }
}
