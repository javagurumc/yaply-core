package ai.claritywalk.entity;

/**
 * Enumeration of supported authentication providers.
 */
public enum AuthProvider {
    /**
     * Traditional email/password authentication
     */
    EMAIL,

    /**
     * Google OAuth 2.0 authentication
     */
    GOOGLE,

    /**
     * Facebook OAuth 2.0 authentication
     */
    FACEBOOK
}
