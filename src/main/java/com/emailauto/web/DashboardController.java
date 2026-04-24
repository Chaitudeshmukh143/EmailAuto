package com.emailauto.web;

import com.emailauto.domain.UserAccount;
import com.emailauto.repository.EmailCampaignRepository;
import com.emailauto.repository.EmailSendLogRepository;
import com.emailauto.service.GoogleOAuthService;
import com.emailauto.web.dto.DashboardResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {
    private final GoogleOAuthService googleOAuthService;
    private final EmailCampaignRepository campaignRepository;
    private final EmailSendLogRepository sendLogRepository;

    public DashboardController(GoogleOAuthService googleOAuthService, EmailCampaignRepository campaignRepository, EmailSendLogRepository sendLogRepository) {
        this.googleOAuthService = googleOAuthService;
        this.campaignRepository = campaignRepository;
        this.sendLogRepository = sendLogRepository;
    }

    @GetMapping("/api/dashboard")
    public ResponseEntity<DashboardResponse> dashboard(HttpSession session) {
        Long userId = (Long) session.getAttribute(GoogleOAuthService.SESSION_USER_ID);
        return googleOAuthService.findUser(userId)
                .map(this::dashboardFor)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new DashboardResponse(false, null, 0, 0, List.of(), List.of())));
    }

    private ResponseEntity<DashboardResponse> dashboardFor(UserAccount user) {
        List<DashboardResponse.CampaignSummary> campaigns = campaignRepository.findTop10ByUserOrderByCreatedAtDesc(user).stream()
                .map(c -> new DashboardResponse.CampaignSummary(c.getId(), c.getSubject(), c.getSentCount(), c.getFailedCount(), c.getCreatedAt()))
                .toList();
        List<DashboardResponse.FailureSummary> failures = sendLogRepository.findTop25ByCampaignUserAndSuccessFalseOrderByCreatedAtDesc(user).stream()
                .map(l -> new DashboardResponse.FailureSummary(l.getRecipientEmail(), l.getErrorMessage(), l.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(new DashboardResponse(
                true,
                user.getEmail(),
                sendLogRepository.countByCampaignUserAndSuccess(user, true),
                sendLogRepository.countByCampaignUserAndSuccess(user, false),
                campaigns,
                failures));
    }
}
