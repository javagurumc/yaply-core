package ai.yaply.security;

import ai.yaply.entity.Profile;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final String secret;
    private final long expirationMs;
    private SecretKey signingKey;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    @PostConstruct
    void initialize() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Profile profile) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(profile.getEmail())
                .claim("userId", profile.getUserId())
                .claim("authProvider", profile.getAuthProvider().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    public String extractEmail(String token) {
        return claims(token).getSubject();
    }

    public boolean isTokenValid(String token, String email) {
        return email.equals(extractEmail(token)) && claims(token).getExpiration().after(new Date());
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
