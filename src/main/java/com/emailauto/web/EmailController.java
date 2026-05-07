package com.emailauto.web;

import com.emailauto.domain.UserAccount;
import com.emailauto.service.BulkEmailRequest;
import com.emailauto.service.BulkEmailService;
import com.emailauto.service.ExcelContactParser;
import com.emailauto.service.GoogleOAuthService;
import com.emailauto.web.dto.BulkEmailResponse;
import com.emailauto.web.dto.ExcelInspectResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class EmailController {
    private static final Logger log = LoggerFactory.getLogger(EmailController.class);
    private final GoogleOAuthService googleOAuthService;
    private final BulkEmailService bulkEmailService;
    private final ExcelContactParser excelContactParser;

    public EmailController(GoogleOAuthService googleOAuthService, BulkEmailService bulkEmailService, ExcelContactParser excelContactParser) {
        this.googleOAuthService = googleOAuthService;
        this.bulkEmailService = bulkEmailService;
        this.excelContactParser = excelContactParser;
    }

    @PostMapping("/api/emails/bulk")
    public ResponseEntity<BulkEmailResponse> sendBulk(@RequestParam MultipartFile file, @RequestParam String subject,
                                                      @RequestParam(required = false) MultipartFile[] attachments,
                                                      @RequestParam String template, @RequestParam(required = false) String cc,
                                                      @RequestParam(required = false) String scheduledAt,
                                                      @RequestParam(required = false) String bcc, @RequestParam(required = false) Long delayMs,
                                                      HttpSession session) throws IOException {
        Long userId = (Long) session.getAttribute(GoogleOAuthService.SESSION_USER_ID);
        UserAccount user = googleOAuthService.findUser(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(bulkEmailService.send(new BulkEmailRequest(
                user,
                file,
                attachments,
                subject,
                template,
                cc,
                bcc,
                parseScheduledAt(scheduledAt),
                delayMs)));
    }

    @PostMapping("/api/emails/inspect")
    public ResponseEntity<ExcelInspectResponse> inspectExcel(@RequestParam MultipartFile file, HttpSession session) throws IOException {
        Long userId = (Long) session.getAttribute(GoogleOAuthService.SESSION_USER_ID);
        if (googleOAuthService.findUser(userId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ExcelContactParser.ExcelMetadata metadata = excelContactParser.inspect(file);
        return ResponseEntity.ok(new ExcelInspectResponse(metadata.headers(), metadata.placeholders(), metadata.rowCount()));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> badRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ErrorResponse> invalidSchedule(DateTimeParseException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Invalid scheduled date/time"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception ex) {
        log.error("Bulk email request failed unexpectedly", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Campaign failed on the server. Please try again."));
    }

    public record ErrorResponse(String message) {
    }

    private Instant parseScheduledAt(String scheduledAt) {
        if (scheduledAt == null || scheduledAt.isBlank()) {
            return null;
        }
        return Instant.parse(scheduledAt);
    }
}
