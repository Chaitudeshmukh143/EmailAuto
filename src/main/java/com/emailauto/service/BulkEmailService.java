package com.emailauto.service;

import com.emailauto.config.AppProperties;
import com.emailauto.domain.EmailCampaign;
import com.emailauto.domain.EmailSendLog;
import com.emailauto.repository.EmailCampaignRepository;
import com.emailauto.repository.EmailSendLogRepository;
import com.emailauto.web.dto.BulkEmailResponse;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BulkEmailService {
    private final ExcelContactParser excelContactParser;
    private final GmailEmailService gmailEmailService;
    private final TemplateService templateService;
    private final EmailCampaignRepository campaignRepository;
    private final EmailSendLogRepository sendLogRepository;
    private final AppProperties properties;

    public BulkEmailService(ExcelContactParser excelContactParser, GmailEmailService gmailEmailService, TemplateService templateService,
                            EmailCampaignRepository campaignRepository, EmailSendLogRepository sendLogRepository, AppProperties properties) {
        this.excelContactParser = excelContactParser;
        this.gmailEmailService = gmailEmailService;
        this.templateService = templateService;
        this.campaignRepository = campaignRepository;
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
        List<Contact> contacts = excelContactParser.parse(request.file());
        EmailCampaign campaign = createCampaign(request);
        int sent = 0;
        int failed = 0;
        long delayMs = request.delayMs() == null ? properties.getMail().getDefaultDelayMs() : Math.max(0, request.delayMs());
        for (Contact contact : contacts) {
            try {
                String renderedSubject = templateService.render(request.subject(), contact);
                String renderedBody = templateService.render(request.template(), contact);
                gmailEmailService.sendEmail(request.user(), contact.email(), renderedSubject, renderedBody);
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
        campaignRepository.save(campaign);
        return new BulkEmailResponse(campaign.getId(), sent, failed);
    }

    @Transactional
    protected EmailCampaign createCampaign(BulkEmailRequest request) {
        EmailCampaign campaign = new EmailCampaign();
        campaign.setUser(request.user());
        campaign.setSubject(request.subject());
        return campaignRepository.save(campaign);
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
