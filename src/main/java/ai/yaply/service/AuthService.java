package ai.yaply.service;

import ai.yaply.dto.LoginRequest;
import ai.yaply.dto.LoginResponse;
import ai.yaply.dto.RegisterRequest;
import ai.yaply.entity.AuthProvider;
import ai.yaply.entity.Profile;
import ai.yaply.event.UserCreatedEvent;
import ai.yaply.repo.ProfileRepository;
import ai.yaply.security.JwtUtil;
import ai.yaply.validation.RegistrationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RegistrationValidator registrationValidator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // Validate registration request using strategy pattern
        registrationValidator.validate(request);

        // Create new profile with hashed password
        UUID profileId = UUID.randomUUID();
        Profile profile = Profile.builder()
                .id(profileId)
                .userId(profileId.toString())
                .email(request.getEmail())
                .responses(request.getResponses())
                .authProvider(AuthProvider.EMAIL)
                .build();

        profile.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        profileRepository.save(profile);

        // Publish event to trigger welcome email
        eventPublisher.publishEvent(new UserCreatedEvent(this, profile));

        // Generate JWT token
        String token = jwtUtil.generateToken(profile.getEmail(), profile.getUserId());

        return LoginResponse.builder()
                .token(token)
                .userId(profile.getUserId())
                .email(profile.getEmail())
                .build();
    }

    public LoginResponse login(LoginRequest request) {
        try {
            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            // Load profile to get userId
            Profile profile = profileRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Generate JWT token
            String token = jwtUtil.generateToken(profile.getEmail(), profile.getUserId());

            return LoginResponse.builder()
                    .token(token)
                    .userId(profile.getUserId())
                    .email(profile.getEmail())
                    .build();
        } catch (AuthenticationException e) {
            throw new IllegalArgumentException("Invalid email or password");
        }
    }
}
