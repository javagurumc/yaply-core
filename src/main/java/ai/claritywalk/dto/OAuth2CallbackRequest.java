package ai.claritywalk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for OAuth 2.0 callback handling.
 * Contains the authorization code from the OAuth provider.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2CallbackRequest {

    /**
     * Authorization code from OAuth provider
     */
    @NotBlank(message = "Authorization code is required")
    private String code;

    /**
     * Optional state parameter for CSRF protection
     */
    private String state;

    /**
     * Optional error from OAuth provider
     */
    private String error;

    /**
     * Optional error description from OAuth provider
     */
    private String errorDescription;
}
