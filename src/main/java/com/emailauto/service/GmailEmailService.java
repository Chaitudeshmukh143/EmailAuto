package com.emailauto.service;

import com.emailauto.domain.UserAccount;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class GmailEmailService {
    private final GoogleOAuthService googleOAuthService;

    public GmailEmailService(GoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }

    public void sendEmail(UserAccount user, String recipient, String cc, String bcc, String subject, String htmlBody, MultipartFile[] attachments)
            throws IOException, MessagingException {
        try {
            gmailClient(user).users().messages().send("me", createMessage(user.getEmail(), recipient, cc, bcc, subject, htmlBody, attachments)).execute();
        } catch (GoogleJsonResponseException ex) {
            if (ex.getStatusCode() != 401) {
                throw ex;
            }
            String refreshToken = googleOAuthService.credentialFor(user).getRefreshToken();
            googleOAuthService.refreshAccessToken(user, refreshToken);
            gmailClient(user).users().messages().send("me", createMessage(user.getEmail(), recipient, cc, bcc, subject, htmlBody, attachments)).execute();
        }
    }

    public void sendEmail(UserAccount user, String recipient, String cc, String bcc, String subject, String htmlBody, StoredAttachment[] attachments)
            throws IOException, MessagingException {
        try {
            gmailClient(user).users().messages().send("me", createMessage(user.getEmail(), recipient, cc, bcc, subject, htmlBody, attachments)).execute();
        } catch (GoogleJsonResponseException ex) {
            if (ex.getStatusCode() != 401) {
                throw ex;
            }
            String refreshToken = googleOAuthService.credentialFor(user).getRefreshToken();
            googleOAuthService.refreshAccessToken(user, refreshToken);
            gmailClient(user).users().messages().send("me", createMessage(user.getEmail(), recipient, cc, bcc, subject, htmlBody, attachments)).execute();
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

    private Message createMessage(String from, String to, String cc, String bcc, String subject, String htmlBody, MultipartFile[] attachments)
            throws MessagingException, IOException {
        MimeMessage email = new MimeMessage(Session.getDefaultInstance(new Properties()));
        email.setFrom(new InternetAddress(from));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        addRecipients(email, jakarta.mail.Message.RecipientType.CC, cc);
        addRecipients(email, jakarta.mail.Message.RecipientType.BCC, bcc);
        email.setSubject(subject, StandardCharsets.UTF_8.name());
        if (attachments == null || attachments.length == 0) {
            email.setContent(htmlBody, "text/html; charset=UTF-8");
        } else {
            email.setContent(createMultipartContent(htmlBody, attachments));
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        String encodedEmail = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray());
        return new Message().setRaw(encodedEmail);
    }

    private Message createMessage(String from, String to, String cc, String bcc, String subject, String htmlBody, StoredAttachment[] attachments)
            throws MessagingException, IOException {
        MimeMessage email = new MimeMessage(Session.getDefaultInstance(new Properties()));
        email.setFrom(new InternetAddress(from));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        addRecipients(email, jakarta.mail.Message.RecipientType.CC, cc);
        addRecipients(email, jakarta.mail.Message.RecipientType.BCC, bcc);
        email.setSubject(subject, StandardCharsets.UTF_8.name());
        if (attachments == null || attachments.length == 0) {
            email.setContent(htmlBody, "text/html; charset=UTF-8");
        } else {
            email.setContent(createMultipartContent(htmlBody, attachments));
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        String encodedEmail = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray());
        return new Message().setRaw(encodedEmail);
    }

    private Multipart createMultipartContent(String htmlBody, MultipartFile[] attachments) throws MessagingException, IOException {
        Multipart multipart = new MimeMultipart();

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
        multipart.addBodyPart(htmlPart);

        for (MultipartFile attachment : attachments) {
            if (attachment == null || attachment.isEmpty()) {
                continue;
            }
            MimeBodyPart attachmentPart = new MimeBodyPart();
            String contentType = StringUtils.hasText(attachment.getContentType())
                    ? attachment.getContentType()
                    : "application/octet-stream";
            DataSource source = new ByteArrayDataSource(attachment.getBytes(), contentType);
            attachmentPart.setDataHandler(new DataHandler(source));
            attachmentPart.setFileName(attachment.getOriginalFilename());
            multipart.addBodyPart(attachmentPart);
        }
        return multipart;
    }

    private Multipart createMultipartContent(String htmlBody, StoredAttachment[] attachments) throws MessagingException, IOException {
        Multipart multipart = new MimeMultipart();

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
        multipart.addBodyPart(htmlPart);

        for (StoredAttachment attachment : attachments) {
            if (attachment == null || attachment.fileData() == null || attachment.fileData().length == 0) {
                continue;
            }
            MimeBodyPart attachmentPart = new MimeBodyPart();
            String contentType = StringUtils.hasText(attachment.contentType())
                    ? attachment.contentType()
                    : "application/octet-stream";
            DataSource source = new ByteArrayDataSource(attachment.fileData(), contentType);
            attachmentPart.setDataHandler(new DataHandler(source));
            attachmentPart.setFileName(attachment.fileName());
            multipart.addBodyPart(attachmentPart);
        }
        return multipart;
    }

    private void addRecipients(MimeMessage email, jakarta.mail.Message.RecipientType type, String addresses) throws MessagingException {
        if (!StringUtils.hasText(addresses)) {
            return;
        }
        for (String address : addresses.split(",")) {
            String trimmed = address.trim();
            if (!trimmed.isEmpty()) {
                email.addRecipient(type, new InternetAddress(trimmed));
            }
        }
    }
}
