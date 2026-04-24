package com.emailauto.service;

import com.emailauto.config.AppProperties;
import com.emailauto.domain.UserAccount;
import com.emailauto.repository.UserAccountRepository;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.GmailScopes;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class GoogleOAuthService {
    public static final String SESSION_USER_ID = "USER_ID";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final AppProperties properties;
    private final UserAccountRepository userAccountRepository;
    private final TokenCryptoService tokenCryptoService;
    private HttpTransport httpTransport;

    public GoogleOAuthService(AppProperties properties, UserAccountRepository userAccountRepository, TokenCryptoService tokenCryptoService) {
        this.properties = properties;
        this.userAccountRepository = userAccountRepository;
        this.tokenCryptoService = tokenCryptoService;
    }

    @PostConstruct
    void init() throws GeneralSecurityException, IOException {
        this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    }

    public String buildAuthorizationUrl(String state) {
        AppProperties.Google google = properties.getOauth().getGoogle();
        return new GoogleAuthorizationCodeRequestUrl(
                google.getClientId(),
                google.getRedirectUri(),
                List.of(GmailScopes.GMAIL_SEND, "openid", "email", "profile"))
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .setState(state)
                .build();
    }

    @Transactional
    public UserAccount exchangeCodeAndUpsertUser(String code) throws IOException, GeneralSecurityException {
        AppProperties.Google google = properties.getOauth().getGoogle();
        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                httpTransport,
                JSON_FACTORY,
                google.getClientId(),
                google.getClientSecret(),
                code,
                google.getRedirectUri())
                .execute();

        GoogleIdToken idToken = verifyIdToken(tokenResponse.getIdToken());
        String email = idToken.getPayload().getEmail();
        UserAccount user = userAccountRepository.findByEmail(email).orElseGet(UserAccount::new);
        user.setEmail(email);
        user.setEncryptedAccessToken(tokenCryptoService.encrypt(tokenResponse.getAccessToken()));
        if (StringUtils.hasText(tokenResponse.getRefreshToken())) {
            user.setEncryptedRefreshToken(tokenCryptoService.encrypt(tokenResponse.getRefreshToken()));
        }
        user.setAccessTokenExpiresAt(expiresAt(tokenResponse.getExpiresInSeconds()));
        user.setUpdatedAt(Instant.now());
        return userAccountRepository.save(user);
    }

    @Transactional
    public Credential credentialFor(UserAccount user) throws IOException {
        String accessToken = tokenCryptoService.decrypt(user.getEncryptedAccessToken());
        String refreshToken = tokenCryptoService.decrypt(user.getEncryptedRefreshToken());
        if (tokenExpiresSoon(user) && StringUtils.hasText(refreshToken)) {
            accessToken = refreshAccessToken(user, refreshToken);
        }
        Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod());
        credential.setAccessToken(accessToken);
        credential.setRefreshToken(refreshToken);
        if (user.getAccessTokenExpiresAt() != null) {
            credential.setExpirationTimeMilliseconds(user.getAccessTokenExpiresAt().toEpochMilli());
        }
        return credential;
    }

    @Transactional
    public String refreshAccessToken(UserAccount user, String refreshToken) throws IOException {
        if (!StringUtils.hasText(refreshToken)) {
            throw new IllegalStateException("Missing refresh token. Reconnect Google account.");
        }
        AppProperties.Google google = properties.getOauth().getGoogle();
        GoogleTokenResponse response = new GoogleRefreshTokenRequest(
                httpTransport,
                JSON_FACTORY,
                refreshToken,
                google.getClientId(),
                google.getClientSecret())
                .execute();
        user.setEncryptedAccessToken(tokenCryptoService.encrypt(response.getAccessToken()));
        user.setAccessTokenExpiresAt(expiresAt(response.getExpiresInSeconds()));
        user.setUpdatedAt(Instant.now());
        userAccountRepository.save(user);
        return response.getAccessToken();
    }

    public Optional<UserAccount> findUser(Long userId) {
        return userId == null ? Optional.empty() : userAccountRepository.findById(userId);
    }

    public HttpTransport httpTransport() {
        return httpTransport;
    }

    private GoogleIdToken verifyIdToken(String rawIdToken) throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(httpTransport, JSON_FACTORY)
                .setAudience(List.of(properties.getOauth().getGoogle().getClientId()))
                .build();
        GoogleIdToken idToken = verifier.verify(rawIdToken);
        if (idToken == null || !Boolean.TRUE.equals(idToken.getPayload().getEmailVerified())) {
            throw new IllegalStateException("Google account email could not be verified");
        }
        return idToken;
    }

    private boolean tokenExpiresSoon(UserAccount user) {
        return user.getAccessTokenExpiresAt() == null || user.getAccessTokenExpiresAt().isBefore(Instant.now().plusSeconds(90));
    }

    private Instant expiresAt(Long expiresInSeconds) {
        return Instant.now().plusSeconds(expiresInSeconds == null ? 3600 : expiresInSeconds);
    }
}
