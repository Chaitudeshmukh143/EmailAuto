package com.emailauto.service;

import com.emailauto.domain.UserAccount;
import org.springframework.web.multipart.MultipartFile;

public record BulkEmailRequest(
        UserAccount user,
        MultipartFile file,
        MultipartFile[] attachments,
        String subject,
        String template,
        String cc,
        String bcc,
        Long delayMs
) {
}
