package com.emailauto.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "campaigns")
public class EmailCampaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;
    @Column(nullable = false)
    private String subject;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String template;
    private String cc;
    private String bcc;
    @Column(nullable = false)
    private long delayMs;
    private Instant scheduledAt;
    private Instant startedAt;
    private Instant completedAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignStatus status = CampaignStatus.DRAFT;
    @Column(nullable = false)
    private String sourceFileName;
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] sourceFileData;
    @Column(nullable = false)
    private int sentCount;
    @Column(nullable = false)
    private int failedCount;
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CampaignAttachment> attachments = new ArrayList<>();
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }
    public String getCc() { return cc; }
    public void setCc(String cc) { this.cc = cc; }
    public String getBcc() { return bcc; }
    public void setBcc(String bcc) { this.bcc = bcc; }
    public long getDelayMs() { return delayMs; }
    public void setDelayMs(long delayMs) { this.delayMs = delayMs; }
    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public CampaignStatus getStatus() { return status; }
    public void setStatus(CampaignStatus status) { this.status = status; }
    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }
    public byte[] getSourceFileData() { return sourceFileData; }
    public void setSourceFileData(byte[] sourceFileData) { this.sourceFileData = sourceFileData; }
    public int getSentCount() { return sentCount; }
    public void setSentCount(int sentCount) { this.sentCount = sentCount; }
    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    public List<CampaignAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<CampaignAttachment> attachments) { this.attachments = attachments; }
    public Instant getCreatedAt() { return createdAt; }
}
