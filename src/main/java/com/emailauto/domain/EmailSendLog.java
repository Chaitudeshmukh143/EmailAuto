package com.emailauto.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "emails")
public class EmailSendLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private EmailCampaign campaign;
    @Column(nullable = false, length = 320)
    private String recipientEmail;
    private String recipientName;
    private String company;
    @Column(nullable = false)
    private boolean success;
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public EmailCampaign getCampaign() { return campaign; }
    public void setCampaign(EmailCampaign campaign) { this.campaign = campaign; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
}
