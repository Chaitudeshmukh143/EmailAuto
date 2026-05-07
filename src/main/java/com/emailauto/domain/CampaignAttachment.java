package com.emailauto.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "campaign_attachments")
public class CampaignAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private EmailCampaign campaign;

    @Column(nullable = false)
    private String fileName;

    private String contentType;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] fileData;

    public Long getId() { return id; }
    public EmailCampaign getCampaign() { return campaign; }
    public void setCampaign(EmailCampaign campaign) { this.campaign = campaign; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }
}
