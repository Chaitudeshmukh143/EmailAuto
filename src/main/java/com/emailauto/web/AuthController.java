package com.emailauto.web;

import com.emailauto.domain.UserAccount;
import com.emailauto.service.GoogleOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
    private static final String SESSION_OAUTH_STATE = "OAUTH_STATE";
    private final SecureRandom secureRandom = new SecureRandom();
    private final GoogleOAuthService googleOAuthService;

    public AuthController(GoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }

    @GetMapping("/auth/google")
    public void login(HttpSession session, HttpServletResponse response) throws IOException {
        String state = newState();
        session.setAttribute(SESSION_OAUTH_STATE, state);
        response.sendRedirect(googleOAuthService.buildAuthorizationUrl(state));
    }

    @GetMapping("/auth/google/callback")
    public void callback(@RequestParam String code, @RequestParam String state, HttpSession session, HttpServletResponse response)
            throws IOException, GeneralSecurityException {
        String expectedState = (String) session.getAttribute(SESSION_OAUTH_STATE);
        if (expectedState == null || !expectedState.equals(state)) {
            response.sendError(400, "Invalid OAuth state");
            return;
        }
        UserAccount user = googleOAuthService.exchangeCodeAndUpsertUser(code);
        session.removeAttribute(SESSION_OAUTH_STATE);
        session.setAttribute(GoogleOAuthService.SESSION_USER_ID, user.getId());
        response.sendRedirect("/");
    }

    @GetMapping("/auth/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/api/me")
    public ResponseEntity<MeResponse> me(HttpSession session) {
        Long userId = (Long) session.getAttribute(GoogleOAuthService.SESSION_USER_ID);
        return googleOAuthService.findUser(userId)
                .map(user -> ResponseEntity.ok(new MeResponse(true, user.getEmail())))
                .orElseGet(() -> ResponseEntity.ok(new MeResponse(false, null)));
    }

    private String newState() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record MeResponse(boolean authenticated, String email) {
    }
}
