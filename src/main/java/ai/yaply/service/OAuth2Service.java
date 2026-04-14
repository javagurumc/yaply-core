package ai.yaply.service;

import ai.yaply.dto.LoginResponse;
import ai.yaply.entity.AuthProvider;
import ai.yaply.entity.Profile;
import ai.yaply.event.UserCreatedEvent;
import ai.yaply.repo.ProfileRepository;
import ai.yaply.security.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * Service for handling OAuth 2.0 authentication flows with Google and Facebook.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final ProfileRepository profileRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${claritywalk.oauth2.google.client-id:}")
    private String googleClientId;

    @Value("${claritywalk.oauth2.google.client-secret:}")
    private String googleClientSecret;

    @Value("${claritywalk.oauth2.google.redirect-uri:}")
    private String googleRedirectUri;

    @Value("${claritywalk.oauth2.facebook.client-id:}")
    private String facebookClientId;

    @Value("${claritywalk.oauth2.facebook.client-secret:}")
    private String facebookClientSecret;

    @Value("${claritywalk.oauth2.facebook.redirect-uri:}")
    private String facebookRedirectUri;

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String FACEBOOK_TOKEN_URL = "https://graph.facebook.com/v18.0/oauth/access_token";
    private static final String FACEBOOK_USERINFO_URL = "https://graph.facebook.com/me?fields=id,name,email";

    /**
     * Handle Google OAuth callback and create/login user.
     *
     * @param code Authorization code from Google
     * @return LoginResponse with JWT token
     */
    @Transactional
    public LoginResponse handleGoogleCallback(String code) {
        log.info("Processing Google OAuth callback");

        log.info("============== OAuth2Service.handleGoogleCallback ============== redirectUri: {}", googleRedirectUri);

        try {
            // Exchange code for access token
            String accessToken = exchangeCodeForToken(code, googleClientId, googleClientSecret,
                    googleRedirectUri, GOOGLE_TOKEN_URL);

            // Get user info from Google
            JsonNode userInfo = getUserInfo(accessToken, GOOGLE_USERINFO_URL);

            String providerUserId = userInfo.get("id").asText();
            String email = userInfo.get("email").asText();
            String name = userInfo.has("name") ? userInfo.get("name").asText() : email;

            // Create or get existing OAuth profile
            Profile profile = createOrGetOAuthProfile(AuthProvider.GOOGLE, providerUserId, email, name);

            // Generate JWT token
            String token = jwtUtil.generateToken(profile.getEmail(), profile.getUserId());

            return LoginResponse.builder()
                    .token(token)
                    .userId(profile.getUserId())
                    .email(profile.getEmail())
                    .build();

        } catch (Exception e) {
            log.error("Error processing Google OAuth callback", e);
            throw new RuntimeException("Failed to authenticate with Google: " + e.getMessage());
        }
    }

    /**
     * Handle Facebook OAuth callback and create/login user.
     *
     * @param code Authorization code from Facebook
     * @return LoginResponse with JWT token
     */
    @Transactional
    public LoginResponse handleFacebookCallback(String code) {
        log.info("Processing Facebook OAuth callback");

        try {
            // Exchange code for access token
            String accessToken = exchangeCodeForToken(code, facebookClientId, facebookClientSecret,
                    facebookRedirectUri, FACEBOOK_TOKEN_URL);

            // Get user info from Facebook
            JsonNode userInfo = getUserInfo(accessToken, FACEBOOK_USERINFO_URL);

            String providerUserId = userInfo.get("id").asText();
            String email = userInfo.has("email") ? userInfo.get("email").asText() : null;
            String name = userInfo.has("name") ? userInfo.get("name").asText() : "Facebook User";

            if (email == null) {
                throw new RuntimeException("Email permission not granted by Facebook user");
            }

            // Create or get existing OAuth profile
            Profile profile = createOrGetOAuthProfile(AuthProvider.FACEBOOK, providerUserId, email, name);

            // Generate JWT token
            String token = jwtUtil.generateToken(profile.getEmail(), profile.getUserId());

            return LoginResponse.builder()
                    .token(token)
                    .userId(profile.getUserId())
                    .email(profile.getEmail())
                    .build();

        } catch (Exception e) {
            log.error("Error processing Facebook OAuth callback", e);
            throw new RuntimeException("Failed to authenticate with Facebook: " + e.getMessage());
        }
    }

    /**
     * Exchange authorization code for access token.
     */
    private String exchangeCodeForToken(String code, String clientId, String clientSecret,
            String redirectUri, String tokenUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            return jsonResponse.get("access_token").asText();
        } catch (Exception e) {
            log.error("Error exchanging code for token", e);
            throw new RuntimeException("Failed to exchange authorization code for access token");
        }
    }

    /**
     * Get user information from OAuth provider.
     */
    private JsonNode getUserInfo(String accessToken, String userInfoUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    userInfoUrl, HttpMethod.GET, entity, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("Error getting user info", e);
            throw new RuntimeException("Failed to fetch user information from OAuth provider");
        }
    }

    /**
     * Create a new OAuth profile or return existing one.
     * Per user requirement: treat accounts as separate per provider (Option C).
     */
    private Profile createOrGetOAuthProfile(AuthProvider provider, String providerUserId,
            String email, String name) {
        // Check if profile exists with this provider + provider user ID
        return profileRepository.findByAuthProviderAndProviderUserId(provider, providerUserId)
                .orElseGet(() -> {
                    // Create new profile
                    UUID profileId = UUID.randomUUID();
                    Profile newProfile = Profile.builder()
                            .id(profileId)
                            .userId(profileId.toString())
                            .email(email)
                            .authProvider(provider)
                            .providerUserId(providerUserId)
                            .providerEmail(email)
                            .responses("{}") // Empty responses for OAuth users
                            .build();

                    log.info("Creating new {} OAuth profile for email: {}", provider, email);
                    Profile savedProfile = profileRepository.save(newProfile);

                    // Publish event to trigger welcome email for new OAuth users
                    eventPublisher.publishEvent(new UserCreatedEvent(this, savedProfile));

                    return savedProfile;
                });
    }
}
