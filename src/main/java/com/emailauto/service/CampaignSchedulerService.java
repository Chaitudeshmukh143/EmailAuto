package com.emailauto.service;

import com.emailauto.domain.CampaignAttachment;
import com.emailauto.domain.CampaignStatus;
import com.emailauto.domain.EmailCampaign;
import com.emailauto.domain.EmailSendLog;
import com.emailauto.repository.EmailCampaignRepository;
import com.emailauto.repository.EmailSendLogRepository;
import jakarta.mail.MessagingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignSchedulerService {
    private final EmailCampaignRepository campaignRepository;
    private final EmailSendLogRepository sendLogRepository;
    private final ExcelContactParser excelContactParser;
    private final GmailEmailService gmailEmailService;
    private final TemplateService templateService;
    private final CampaignSchedulerService self;

    public CampaignSchedulerService(EmailCampaignRepository campaignRepository, EmailSendLogRepository sendLogRepository,
                                    ExcelContactParser excelContactParser,
                                    GmailEmailService gmailEmailService, TemplateService templateService,
                                    @Lazy CampaignSchedulerService self) {
        this.campaignRepository = campaignRepository;
        this.sendLogRepository = sendLogRepository;
        this.excelContactParser = excelContactParser;
        this.gmailEmailService = gmailEmailService;
        this.templateService = templateService;
        this.self = self;
    }

    @Scheduled(fixedDelayString = "${app.mail.scheduler-delay-ms:5000}")
    public void processDueCampaigns() {
        List<EmailCampaign> dueCampaigns = campaignRepository.findDueCampaigns(
                List.copyOf(EnumSet.of(CampaignStatus.SCHEDULED, CampaignStatus.PROCESSING)),
                Instant.now());
        for (EmailCampaign campaign : dueCampaigns) {
            self.processCampaign(campaign.getId());
        }
    }

    @Transactional
    public void processCampaign(Long campaignId) {
        EmailCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalStateException("Campaign not found"));
        if (campaign.getStatus() != CampaignStatus.SCHEDULED && campaign.getStatus() != CampaignStatus.PROCESSING) {
            return;
        }

        try {
            List<Contact> contacts = excelContactParser.parse(new ByteArrayInputStream(campaign.getSourceFileData()), campaign.getSourceFileName());
            if (contacts.isEmpty()) {
                campaign.setStatus(CampaignStatus.FAILED);
                campaign.setCompletedAt(Instant.now());
                campaignRepository.save(campaign);
                return;
            }

            int nextIndex = campaign.getSentCount() + campaign.getFailedCount();
            if (nextIndex >= contacts.size()) {
                campaign.setStatus(CampaignStatus.COMPLETED);
                if (campaign.getStartedAt() == null) {
                    campaign.setStartedAt(campaign.getCreatedAt());
                }
                campaign.setCompletedAt(Instant.now());
                campaignRepository.save(campaign);
                return;
            }

            if (campaign.getStartedAt() == null) {
                campaign.setStartedAt(Instant.now());
            }
            campaign.setStatus(CampaignStatus.PROCESSING);

            Contact contact = contacts.get(nextIndex);
            StoredAttachment[] attachments = campaign.getAttachments().stream()
                    .map(this::toStoredAttachment)
                    .toArray(StoredAttachment[]::new);

            try {
                String renderedSubject = templateService.renderSubject(campaign.getSubject(), contact);
                String renderedBody = templateService.renderBody(campaign.getTemplate(), contact);
                gmailEmailService.sendEmail(campaign.getUser(), contact.email(), campaign.getCc(), campaign.getBcc(), renderedSubject, renderedBody, attachments);
                campaign.setSentCount(campaign.getSentCount() + 1);
                saveLog(campaign, contact, true, null);
            } catch (IOException | MessagingException | RuntimeException ex) {
                campaign.setFailedCount(campaign.getFailedCount() + 1);
                saveLog(campaign, contact, false, ex.getMessage());
            }

            boolean completed = (campaign.getSentCount() + campaign.getFailedCount()) >= contacts.size();
            if (completed) {
                campaign.setStatus(CampaignStatus.COMPLETED);
                campaign.setCompletedAt(Instant.now());
            } else {
                campaign.setStatus(CampaignStatus.SCHEDULED);
                campaign.setScheduledAt(Instant.now().plusMillis(Math.max(0, campaign.getDelayMs())));
            }
        } catch (IOException ex) {
            campaign.setStatus(CampaignStatus.FAILED);
            campaign.setCompletedAt(Instant.now());
            throw new IllegalStateException("Unable to process scheduled campaign", ex);
        } finally {
            campaignRepository.save(campaign);
        }
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

    private StoredAttachment toStoredAttachment(CampaignAttachment attachment) {
        return new StoredAttachment(attachment.getFileName(), attachment.getContentType(), attachment.getFileData());
    }
}
