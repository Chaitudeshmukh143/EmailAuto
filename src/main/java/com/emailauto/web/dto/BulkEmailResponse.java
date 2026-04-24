package com.emailauto.web.dto;

public record BulkEmailResponse(long campaignId, int sent, int failed) {
}
