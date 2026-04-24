package com.emailauto.service;

import com.emailauto.domain.UserAccount;
import org.springframework.web.multipart.MultipartFile;

public record BulkEmailRequest(UserAccount user, MultipartFile file, String subject, String template, Long delayMs) {
}
