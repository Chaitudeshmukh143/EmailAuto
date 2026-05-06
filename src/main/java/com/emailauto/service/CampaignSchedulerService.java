package com.emailauto.service;

import com.emailauto.domain.CampaignAttachment;
import com.emailauto.domain.CampaignStatus;
import com.emailauto.domain.EmailCampaign;
import com.emailauto.repository.EmailCampaignRepository;
import jakarta.mail.MessagingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignSchedulerService {
    private final EmailCampaignRepository campaignRepository;
    private final ExcelContactParser excelContactParser;
    private final GmailEmailService gmailEmailService;
    private final TemplateService templateService;
    private final CampaignSchedulerService self;

    public CampaignSchedulerService(EmailCampaignRepository campaignRepository, ExcelContactParser excelContactParser,
                                    GmailEmailService gmailEmailService, TemplateService templateService,
                                    @Lazy CampaignSchedulerService self) {
        this.campaignRepository = campaignRepository;
        this.excelContactParser = excelContactParser;
        this.gmailEmailService = gmailEmailService;
        this.templateService = templateService;
        this.self = self;
    }

    @Scheduled(fixedDelayString = "${app.mail.scheduler-delay-ms:30000}")
    public void processDueCampaigns() {
        List<EmailCampaign> dueCampaigns = campaignRepository.findDueCampaigns(CampaignStatus.SCHEDULED, Instant.now());
        for (EmailCampaign campaign : dueCampaigns) {
            self.processCampaign(campaign.getId());
        }
    }

    @Transactional
    public void processCampaign(Long campaignId) {
        EmailCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalStateException("Campaign not found"));
        if (campaign.getStatus() != CampaignStatus.SCHEDULED && campaign.getStatus() != CampaignStatus.DRAFT) {
            return;
        }
        campaign.setStatus(CampaignStatus.PROCESSING);
        campaign.setStartedAt(Instant.now());
        campaignRepository.save(campaign);

        int sent = 0;
        int failed = 0;
        try {
            List<Contact> contacts = excelContactParser.parse(new ByteArrayInputStream(campaign.getSourceFileData()), campaign.getSourceFileName());
            StoredAttachment[] attachments = campaign.getAttachments().stream()
                    .map(this::toStoredAttachment)
                    .toArray(StoredAttachment[]::new);

            for (Contact contact : contacts) {
                try {
                    String renderedSubject = templateService.renderSubject(campaign.getSubject(), contact);
                    String renderedBody = templateService.renderBody(campaign.getTemplate(), contact);
                    gmailEmailService.sendEmail(campaign.getUser(), contact.email(), campaign.getCc(), campaign.getBcc(), renderedSubject, renderedBody, attachments);
                    sent++;
                } catch (IOException | MessagingException | RuntimeException ex) {
                    failed++;
                }
                sleep(campaign.getDelayMs());
            }
            campaign.setSentCount(sent);
            campaign.setFailedCount(failed);
            campaign.setStatus(CampaignStatus.COMPLETED);
            campaign.setCompletedAt(Instant.now());
        } catch (IOException ex) {
            campaign.setStatus(CampaignStatus.FAILED);
            campaign.setCompletedAt(Instant.now());
            throw new IllegalStateException("Unable to process scheduled campaign", ex);
        } finally {
            campaignRepository.save(campaign);
        }
    }

    private StoredAttachment toStoredAttachment(CampaignAttachment attachment) {
        return new StoredAttachment(attachment.getFileName(), attachment.getContentType(), attachment.getFileData());
    }

    private void sleep(long delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Scheduled campaign interrupted", ex);
        }
    }
}
