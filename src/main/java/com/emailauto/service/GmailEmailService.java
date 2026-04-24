package com.emailauto.service;

import com.emailauto.domain.UserAccount;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import org.springframework.stereotype.Service;

@Service
public class GmailEmailService {
    private final GoogleOAuthService googleOAuthService;

    public GmailEmailService(GoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }

    public void sendEmail(UserAccount user, String recipient, String subject, String htmlBody) throws IOException, MessagingException {
        try {
            gmailClient(user).users().messages().send("me", createMessage(user.getEmail(), recipient, subject, htmlBody)).execute();
        } catch (GoogleJsonResponseException ex) {
            if (ex.getStatusCode() != 401) {
                throw ex;
            }
            String refreshToken = googleOAuthService.credentialFor(user).getRefreshToken();
            googleOAuthService.refreshAccessToken(user, refreshToken);
            gmailClient(user).users().messages().send("me", createMessage(user.getEmail(), recipient, subject, htmlBody)).execute();
        }
    }

    private Gmail gmailClient(UserAccount user) throws IOException {
        return new Gmail.Builder(
                googleOAuthService.httpTransport(),
                GsonFactory.getDefaultInstance(),
                googleOAuthService.credentialFor(user))
                .setApplicationName("EmailAuto")
                .build();
    }

    private Message createMessage(String from, String to, String subject, String htmlBody) throws MessagingException, IOException {
        MimeMessage email = new MimeMessage(Session.getDefaultInstance(new Properties()));
        email.setFrom(new InternetAddress(from));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject, StandardCharsets.UTF_8.name());
        email.setContent(htmlBody, "text/html; charset=UTF-8");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        String encodedEmail = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray());
        return new Message().setRaw(encodedEmail);
    }
}
