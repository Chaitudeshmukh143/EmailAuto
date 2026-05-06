package com.emailauto.service;

import com.emailauto.config.AppProperties;
import com.emailauto.domain.CampaignAttachment;
import com.emailauto.domain.CampaignStatus;
import com.emailauto.domain.EmailCampaign;
import com.emailauto.domain.EmailSendLog;
import com.emailauto.repository.CampaignAttachmentRepository;
import com.emailauto.repository.EmailCampaignRepository;
import com.emailauto.repository.EmailSendLogRepository;
import com.emailauto.web.dto.BulkEmailResponse;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BulkEmailService {
    private final ExcelContactParser excelContactParser;
    private final GmailEmailService gmailEmailService;
    private final TemplateService templateService;
    private final EmailCampaignRepository campaignRepository;
    private final CampaignAttachmentRepository campaignAttachmentRepository;
    private final EmailSendLogRepository sendLogRepository;
    private final AppProperties properties;

    public BulkEmailService(ExcelContactParser excelContactParser, GmailEmailService gmailEmailService, TemplateService templateService,
                            EmailCampaignRepository campaignRepository, CampaignAttachmentRepository campaignAttachmentRepository,
                            EmailSendLogRepository sendLogRepository, AppProperties properties) {
        this.excelContactParser = excelContactParser;
        this.gmailEmailService = gmailEmailService;
        this.templateService = templateService;
        this.campaignRepository = campaignRepository;
        this.campaignAttachmentRepository = campaignAttachmentRepository;
        this.sendLogRepository = sendLogRepository;
        this.properties = properties;
    }

    public BulkEmailResponse send(BulkEmailRequest request) throws IOException {
        if (!StringUtils.hasText(request.subject())) {
            throw new IllegalArgumentException("Subject is required");
        }
        if (!StringUtils.hasText(request.template())) {
            throw new IllegalArgumentException("Email template is required");
        }
        EmailCampaign campaign = createCampaign(request);
        if (request.scheduledAt() != null && request.scheduledAt().isAfter(Instant.now())) {
            return new BulkEmailResponse(campaign.getId(), 0, 0);
        }

        List<Contact> contacts = excelContactParser.parse(request.file());
        int sent = 0;
        int failed = 0;
        long delayMs = campaign.getDelayMs();
        for (Contact contact : contacts) {
            try {
                String renderedSubject = templateService.renderSubject(request.subject(), contact);
                String renderedBody = templateService.renderBody(request.template(), contact);
                gmailEmailService.sendEmail(
                        request.user(),
                        contact.email(),
                        request.cc(),
                        request.bcc(),
                        renderedSubject,
                        renderedBody,
                        request.attachments());
                sent++;
                saveLog(campaign, contact, true, null);
            } catch (IOException | MessagingException | RuntimeException ex) {
                failed++;
                saveLog(campaign, contact, false, ex.getMessage());
            }
            sleep(delayMs);
        }
        campaign.setSentCount(sent);
        campaign.setFailedCount(failed);
        campaign.setStatus(CampaignStatus.COMPLETED);
        campaign.setStartedAt(campaign.getStartedAt() == null ? Instant.now() : campaign.getStartedAt());
        campaign.setCompletedAt(Instant.now());
        campaignRepository.save(campaign);
        return new BulkEmailResponse(campaign.getId(), sent, failed);
    }

    @Transactional
    protected EmailCampaign createCampaign(BulkEmailRequest request) throws IOException {
        EmailCampaign campaign = new EmailCampaign();
        campaign.setUser(request.user());
        campaign.setSubject(request.subject());
        campaign.setTemplate(request.template());
        campaign.setCc(request.cc());
        campaign.setBcc(request.bcc());
        campaign.setDelayMs(request.delayMs() == null ? properties.getMail().getDefaultDelayMs() : Math.max(0, request.delayMs()));
        campaign.setScheduledAt(request.scheduledAt());
        campaign.setStatus(request.scheduledAt() != null && request.scheduledAt().isAfter(Instant.now()) ? CampaignStatus.SCHEDULED : CampaignStatus.DRAFT);
        campaign.setSourceFileName(request.file().getOriginalFilename());
        campaign.setSourceFileData(request.file().getBytes());
        EmailCampaign savedCampaign = campaignRepository.save(campaign);
        if (request.attachments() != null) {
            List<CampaignAttachment> attachments = new ArrayList<>();
            for (MultipartFile attachment : request.attachments()) {
                if (attachment == null || attachment.isEmpty()) {
                    continue;
                }
                CampaignAttachment stored = new CampaignAttachment();
                stored.setCampaign(savedCampaign);
                stored.setFileName(attachment.getOriginalFilename());
                stored.setContentType(attachment.getContentType());
                stored.setFileData(attachment.getBytes());
                attachments.add(stored);
            }
            savedCampaign.setAttachments(campaignAttachmentRepository.saveAll(attachments));
        }
        return savedCampaign;
    }

    @Transactional
    protected void saveLog(EmailCampaign campaign, Contact contact, boolean success, String error) {
        EmailSendLog log = new EmailSendLog();
        log.setCampaign(campaign);
        log.setRecipientEmail(contact.email());
        log.setRecipientName(contact.name());
        log.setCompany(contact.company());
        log.setSuccess(success);
        log.setErrorMessage(error);
        sendLogRepository.save(log);
    }

    private void sleep(long delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bulk send interrupted", ex);
        }
    }
}
