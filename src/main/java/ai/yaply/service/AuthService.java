package ai.yaply.service;

import ai.yaply.dto.LoginRequest;
import ai.yaply.dto.LoginResponse;
import ai.yaply.dto.RegisterRequest;
import ai.yaply.entity.AuthProvider;
import ai.yaply.entity.Profile;
import ai.yaply.repo.ProfileRepository;
import ai.yaply.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public LoginResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (profileRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }

        String responsesJson = normalizeResponses(request.getResponses());
        Profile profile = Profile.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID().toString())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .authProvider(AuthProvider.EMAIL)
                .responses(responsesJson)
                .build();

        Profile savedProfile = profileRepository.save(profile);
        return response(savedProfile);
    }

    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        Profile profile = profileRepository.findByEmailAndAuthProvider(email, AuthProvider.EMAIL)
                .filter(p -> p.getPasswordHash() != null && !p.getPasswordHash().isBlank())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        return response(profile);
    }

    private LoginResponse response(Profile profile) {
        return LoginResponse.builder()
                .token(jwtService.generateToken(profile))
                .userId(profile.getUserId())
                .email(profile.getEmail())
                .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeResponses(String responses) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(
                    responses,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
            return objectMapper.writeValueAsString(parsed);
        } catch (Exception e) {
            throw new IllegalArgumentException("Responses must be valid JSON object");
        }
    }
}
