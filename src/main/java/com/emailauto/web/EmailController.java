package com.emailauto.web;

import com.emailauto.domain.UserAccount;
import com.emailauto.service.BulkEmailRequest;
import com.emailauto.service.BulkEmailService;
import com.emailauto.service.GoogleOAuthService;
import com.emailauto.web.dto.BulkEmailResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class EmailController {
    private final GoogleOAuthService googleOAuthService;
    private final BulkEmailService bulkEmailService;

    public EmailController(GoogleOAuthService googleOAuthService, BulkEmailService bulkEmailService) {
        this.googleOAuthService = googleOAuthService;
        this.bulkEmailService = bulkEmailService;
    }

    @PostMapping("/api/emails/bulk")
    public ResponseEntity<BulkEmailResponse> sendBulk(@RequestParam MultipartFile file, @RequestParam String subject,
                                                      @RequestParam String template, @RequestParam(required = false) Long delayMs,
                                                      HttpSession session) throws IOException {
        Long userId = (Long) session.getAttribute(GoogleOAuthService.SESSION_USER_ID);
        UserAccount user = googleOAuthService.findUser(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(bulkEmailService.send(new BulkEmailRequest(user, file, subject, template, delayMs)));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> badRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    public record ErrorResponse(String message) {
    }
}
