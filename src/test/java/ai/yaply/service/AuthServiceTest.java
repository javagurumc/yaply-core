package ai.yaply.service;

import ai.yaply.dto.LoginRequest;
import ai.yaply.dto.RegisterRequest;
import ai.yaply.entity.AuthProvider;
import ai.yaply.entity.Profile;
import ai.yaply.repo.ProfileRepository;
import ai.yaply.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private ProfileRepository profileRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        profileRepository = mock(ProfileRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authenticationManager = mock(AuthenticationManager.class);
        jwtService = mock(JwtService.class);
        authService = new AuthService(
                profileRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                new ObjectMapper());
    }

    @Test
    void givenNewEmail_whenRegister_thenCreatesEmailProfileAndReturnsToken() {
        var request = new RegisterRequest();
        request.setEmail("  USER@example.COM ");
        request.setPassword("secret1");
        request.setResponses("{\"goal\":\"practice\"}");

        when(profileRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret1")).thenReturn("hashed-password");
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(Profile.class))).thenReturn("jwt-token");

        var response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getUserId()).isNotBlank();

        verify(profileRepository).save(argThat(profile ->
                profile.getEmail().equals("user@example.com")
                        && profile.getPasswordHash().equals("hashed-password")
                        && profile.getAuthProvider() == AuthProvider.EMAIL
                        && profile.getResponses().contains("practice")));
    }

    @Test
    void givenExistingEmail_whenRegister_thenRejectsDuplicate() {
        var request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("secret1");
        request.setResponses("{}");

        when(profileRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(Profile.builder().email("user@example.com").build()));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is already registered");
    }

    @Test
    void givenInvalidResponsesJson_whenRegister_thenRejectsRequest() {
        var request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("secret1");
        request.setResponses("not-json");

        when(profileRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Responses must be valid JSON object");
    }

    @Test
    void givenValidCredentials_whenLogin_thenAuthenticatesAndReturnsToken() {
        var request = new LoginRequest();
        request.setEmail("USER@example.COM");
        request.setPassword("secret1");

        var profile = Profile.builder()
                .email("user@example.com")
                .userId("user-123")
                .passwordHash("hashed-password")
                .authProvider(AuthProvider.EMAIL)
                .build();

        when(profileRepository.findByEmailAndAuthProvider("user@example.com", AuthProvider.EMAIL))
                .thenReturn(Optional.of(profile));
        when(jwtService.generateToken(profile)).thenReturn("jwt-token");

        var response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUserId()).isEqualTo("user-123");
        assertThat(response.getEmail()).isEqualTo("user@example.com");

        verify(authenticationManager).authenticate(argThat(authentication ->
                authentication instanceof UsernamePasswordAuthenticationToken
                        && authentication.getName().equals("user@example.com")
                        && authentication.getCredentials().equals("secret1")));
    }

    @Test
    void givenInvalidCredentials_whenLogin_thenRejectsRequest() {
        var request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
}
