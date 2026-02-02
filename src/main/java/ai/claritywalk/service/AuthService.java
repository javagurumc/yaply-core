package ai.claritywalk.service;

import ai.claritywalk.dto.LoginRequest;
import ai.claritywalk.dto.LoginResponse;
import ai.claritywalk.dto.RegisterRequest;
import ai.claritywalk.entity.Profile;
import ai.claritywalk.repo.ProfileRepository;
import ai.claritywalk.security.JwtUtil;
import ai.claritywalk.validation.RegistrationValidator;
import lombok.RequiredArgsConstructor;
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
                .build();

        profile.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        profileRepository.save(profile);

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
