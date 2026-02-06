package ai.claritywalk.service;

import ai.claritywalk.dto.LoginRequest;
import ai.claritywalk.dto.LoginResponse;
import ai.claritywalk.dto.RegisterRequest;
import ai.claritywalk.entity.Profile;
import ai.claritywalk.repo.ProfileRepository;
import ai.claritywalk.security.JwtUtil;
import ai.claritywalk.validation.RegistrationValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

        @Mock
        private ProfileRepository profileRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private JwtUtil jwtUtil;

        @Mock
        private AuthenticationManager authenticationManager;

        @Mock
        private RegistrationValidator registrationValidator;

        @Mock
        private org.springframework.context.ApplicationEventPublisher eventPublisher;

        @InjectMocks
        private AuthService authService;

        private RegisterRequest registerRequest;
        private LoginRequest loginRequest;
        private Profile testProfile;
        private static final String TEST_EMAIL = "test@example.com";
        private static final String TEST_PASSWORD = "password123";
        private static final String TEST_TOKEN = "jwt.token.here";
        private static final String TEST_USER_ID = "test-user-id";

        @BeforeEach
        void setUp() {
                registerRequest = new RegisterRequest();
                registerRequest.setEmail(TEST_EMAIL);
                registerRequest.setPassword(TEST_PASSWORD);
                registerRequest.setResponses("{}");

                loginRequest = new LoginRequest();
                loginRequest.setEmail(TEST_EMAIL);
                loginRequest.setPassword(TEST_PASSWORD);

                testProfile = Profile.builder()
                                .id(UUID.randomUUID())
                                .userId(TEST_USER_ID)
                                .email(TEST_EMAIL)
                                .responses("{}")
                                .build();
                testProfile.setPasswordHash("hashed_password");
        }

        // ==================== Registration Tests ====================

        @Test
        void register_Success_ReturnsLoginResponse() {
                // Arrange
                doNothing().when(registrationValidator).validate(registerRequest);
                when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn("hashed_password");
                when(profileRepository.save(any(Profile.class))).thenReturn(testProfile);
                when(jwtUtil.generateToken(anyString(), anyString())).thenReturn(TEST_TOKEN);

                // Act
                LoginResponse response = authService.register(registerRequest);

                // Assert
                assertNotNull(response);
                assertEquals(TEST_TOKEN, response.getToken());
                assertEquals(TEST_EMAIL, response.getEmail());
                assertNotNull(response.getUserId());

                // Verify interactions
                verify(registrationValidator, times(1)).validate(registerRequest);
                verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
                verify(profileRepository, times(1)).save(any(Profile.class));
                verify(jwtUtil, times(1)).generateToken(anyString(), anyString());
        }

        @Test
        void register_DuplicateEmail_ThrowsIllegalArgumentException() {
                // Arrange
                doThrow(new IllegalArgumentException("Email already registered"))
                                .when(registrationValidator).validate(registerRequest);

                // Act & Assert
                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> authService.register(registerRequest));

                assertEquals("Email already registered", exception.getMessage());

                // Verify that no profile was saved
                verify(registrationValidator, times(1)).validate(registerRequest);
                verify(profileRepository, never()).save(any(Profile.class));
                verify(jwtUtil, never()).generateToken(anyString(), anyString());
        }

        @Test
        void register_PasswordIsHashed() {
                // Arrange
                doNothing().when(registrationValidator).validate(registerRequest);
                when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn("hashed_password");
                when(profileRepository.save(any(Profile.class))).thenReturn(testProfile);
                when(jwtUtil.generateToken(anyString(), anyString())).thenReturn(TEST_TOKEN);

                // Act
                authService.register(registerRequest);

                // Assert - verify password was encoded
                verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
                verify(profileRepository, times(1)).save(argThat(profile -> profile.getPasswordHash() != null &&
                                !profile.getPasswordHash().equals(TEST_PASSWORD)));
        }

        // ==================== Login Tests ====================

        @Test
        void login_ValidCredentials_ReturnsLoginResponse() {
                // Arrange
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(null); // Authentication successful
                when(profileRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testProfile));
                when(jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID)).thenReturn(TEST_TOKEN);

                // Act
                LoginResponse response = authService.login(loginRequest);

                // Assert
                assertNotNull(response);
                assertEquals(TEST_TOKEN, response.getToken());
                assertEquals(TEST_EMAIL, response.getEmail());
                assertEquals(TEST_USER_ID, response.getUserId());

                // Verify interactions
                verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
                verify(profileRepository, times(1)).findByEmail(TEST_EMAIL);
                verify(jwtUtil, times(1)).generateToken(TEST_EMAIL, TEST_USER_ID);
        }

        @Test
        void login_InvalidCredentials_ThrowsIllegalArgumentException() {
                // Arrange
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenThrow(new BadCredentialsException("Bad credentials"));

                // Act & Assert
                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> authService.login(loginRequest));

                assertEquals("Invalid email or password", exception.getMessage());

                // Verify that profile lookup and token generation were not called
                verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
                verify(profileRepository, never()).findByEmail(anyString());
                verify(jwtUtil, never()).generateToken(anyString(), anyString());
        }

        @Test
        void login_UserNotFoundAfterAuthentication_ThrowsIllegalArgumentException() {
                // Arrange
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(null); // Authentication successful
                when(profileRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

                // Act & Assert
                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> authService.login(loginRequest));

                assertEquals("User not found", exception.getMessage());

                // Verify interactions
                verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
                verify(profileRepository, times(1)).findByEmail(TEST_EMAIL);
                verify(jwtUtil, never()).generateToken(anyString(), anyString());
        }

        @Test
        void login_AuthenticationTokenContainsCorrectCredentials() {
                // Arrange
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(null);
                when(profileRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testProfile));
                when(jwtUtil.generateToken(anyString(), anyString())).thenReturn(TEST_TOKEN);

                // Act
                authService.login(loginRequest);

                // Assert - verify authentication was called with correct credentials
                verify(authenticationManager, times(1)).authenticate(
                                argThat(token -> token instanceof UsernamePasswordAuthenticationToken &&
                                                token.getPrincipal().equals(TEST_EMAIL) &&
                                                token.getCredentials().equals(TEST_PASSWORD)));
        }

        @Test
        void login_GeneratesTokenWithCorrectParameters() {
                // Arrange
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(null);
                when(profileRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testProfile));
                when(jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID)).thenReturn(TEST_TOKEN);

                // Act
                authService.login(loginRequest);

                // Assert - verify token generation with correct email and userId
                verify(jwtUtil, times(1)).generateToken(TEST_EMAIL, TEST_USER_ID);
        }
}
