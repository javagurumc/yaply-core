package ai.yaply.controller;

import ai.yaply.dto.LoginRequest;
import ai.yaply.dto.LoginResponse;
import ai.yaply.dto.RegisterRequest;
import ai.yaply.service.AuthService;
import ai.yaply.service.OAuth2Service;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuth2Service oauth2Service;

    @Value("${claritywalk.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            LoginResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * Google OAuth 2.0 callback endpoint.
     * Receives authorization code from Google, completes authentication,
     * and redirects to frontend with authentication tokens.
     */
    @GetMapping("/oauth2/callback/google")
    public void googleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String state,
            HttpServletResponse response) throws IOException {

        // Handle OAuth errors
        if (error != null) {
            log.error("Google OAuth error: {}", error);
            String errorRedirect = buildErrorRedirect(state, "OAuth authentication failed: " + error);
            response.sendRedirect(errorRedirect);
            return;
        }

        // Validate authorization code
        if (code == null) {
            log.error("Google OAuth callback missing authorization code");
            String errorRedirect = buildErrorRedirect(state, "Authorization code is missing");
            response.sendRedirect(errorRedirect);
            return;
        }

        try {
            // Process OAuth callback and get authentication response
            LoginResponse loginResponse = oauth2Service.handleGoogleCallback(code);

            // Build redirect URL with authentication data
            String redirectUrl = buildSuccessRedirect(state, loginResponse);
            log.info("Google OAuth successful for user: {}, redirecting to: {}",
                    loginResponse.getEmail(), redirectUrl);

            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("Google OAuth authentication failed", e);
            String errorRedirect = buildErrorRedirect(state, "Authentication failed: " + e.getMessage());
            response.sendRedirect(errorRedirect);
        }
    }

    /**
     * Facebook OAuth 2.0 callback endpoint.
     * Receives authorization code from Facebook, completes authentication,
     * and redirects to frontend with authentication tokens.
     */
    @GetMapping("/oauth2/callback/facebook")
    public void facebookCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String state,
            HttpServletResponse response) throws IOException {

        // Handle OAuth errors
        if (error != null) {
            log.error("Facebook OAuth error: {}", error);
            String errorRedirect = buildErrorRedirect(state, "OAuth authentication failed: " + error);
            response.sendRedirect(errorRedirect);
            return;
        }

        // Validate authorization code
        if (code == null) {
            log.error("Facebook OAuth callback missing authorization code");
            String errorRedirect = buildErrorRedirect(state, "Authorization code is missing");
            response.sendRedirect(errorRedirect);
            return;
        }

        try {
            // Process OAuth callback and get authentication response
            LoginResponse loginResponse = oauth2Service.handleFacebookCallback(code);

            // Build redirect URL with authentication data
            String redirectUrl = buildSuccessRedirect(state, loginResponse);
            log.info("Facebook OAuth successful for user: {}, redirecting to: {}",
                    loginResponse.getEmail(), redirectUrl);

            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("Facebook OAuth authentication failed", e);
            String errorRedirect = buildErrorRedirect(state, "Authentication failed: " + e.getMessage());
            response.sendRedirect(errorRedirect);
        }
    }

    /**
     * Build successful OAuth redirect URL with authentication tokens.
     */
    private String buildSuccessRedirect(String state, LoginResponse loginResponse) {
        // Use state parameter if provided (frontend callback URL), otherwise use
        // default
        String baseUrl = (state != null && !state.isEmpty()) ? state : frontendUrl + "/auth/callback";

        return UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("token", loginResponse.getToken())
                .queryParam("userId", loginResponse.getUserId())
                .queryParam("email", loginResponse.getEmail())
                .build()
                .toUriString();
    }

    /**
     * Build error redirect URL.
     */
    private String buildErrorRedirect(String state, String errorMessage) {
        // Use state parameter if provided, otherwise use default
        String baseUrl = (state != null && !state.isEmpty()) ? state : frontendUrl + "/auth/callback";

        return UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("error", errorMessage)
                .build()
                .toUriString();
    }

    //trigger
}
