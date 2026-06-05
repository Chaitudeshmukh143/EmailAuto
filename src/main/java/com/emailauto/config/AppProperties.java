package com.emailauto.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    @NotBlank
    private String baseUrl;
    @NotBlank
    private String tokenEncryptionKey;
    private OAuth oauth = new OAuth();
    private Mail mail = new Mail();
    private Cloudinary cloudinary = new Cloudinary();

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getTokenEncryptionKey() { return tokenEncryptionKey; }
    public void setTokenEncryptionKey(String tokenEncryptionKey) { this.tokenEncryptionKey = tokenEncryptionKey; }
    public OAuth getOauth() { return oauth; }
    public void setOauth(OAuth oauth) { this.oauth = oauth; }
    public Mail getMail() { return mail; }
    public void setMail(Mail mail) { this.mail = mail; }
    public Cloudinary getCloudinary() { return cloudinary; }
    public void setCloudinary(Cloudinary cloudinary) { this.cloudinary = cloudinary; }

    public static class OAuth {
        private Google google = new Google();
        public Google getGoogle() { return google; }
        public void setGoogle(Google google) { this.google = google; }
    }

    public static class Google {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    }

    public static class Mail {
        private long defaultDelayMs = 60000;
        public long getDefaultDelayMs() { return defaultDelayMs; }
        public void setDefaultDelayMs(long defaultDelayMs) { this.defaultDelayMs = defaultDelayMs; }
    }

    public static class Cloudinary {
        private String cloudName;
        private String apiKey;
        private String apiSecret;
        private String folder = "email-auto/excel";
        private String allowedDownloadHost = "res.cloudinary.com";

        public String getCloudName() { return cloudName; }
        public void setCloudName(String cloudName) { this.cloudName = cloudName; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getApiSecret() { return apiSecret; }
        public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
        public String getFolder() { return folder; }
        public void setFolder(String folder) { this.folder = folder; }
        public String getAllowedDownloadHost() { return allowedDownloadHost; }
        public void setAllowedDownloadHost(String allowedDownloadHost) { this.allowedDownloadHost = allowedDownloadHost; }
    }
}
