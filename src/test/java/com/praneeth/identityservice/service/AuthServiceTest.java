package com.praneeth.identityservice.service;

import com.praneeth.identityservice.dto.AuthResponse;
import com.praneeth.identityservice.dto.CompleteRegistrationRequest;
import com.praneeth.identityservice.dto.LoginRequest;
import com.praneeth.identityservice.dto.RegistrationEmailRequest;
import com.praneeth.identityservice.dto.UserResponse;
import com.praneeth.identityservice.entity.RegistrationToken;
import com.praneeth.identityservice.entity.Role;
import com.praneeth.identityservice.entity.User;
import com.praneeth.identityservice.entity.UserStatus;
import com.praneeth.identityservice.event.RegistrationRequestedEvent;
import com.praneeth.identityservice.mapper.UserMapper;
import com.praneeth.identityservice.repository.RegistrationTokenRepository;
import com.praneeth.identityservice.repository.UserRepository;
import com.praneeth.identityservice.security.JwtService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegistrationTokenRepository registrationTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void registrationRequestStoresHashedToken() {
        String email = "user@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(registrationTokenRepository.findAllByEmailAndUsedFalse(email))
                .thenReturn(List.of());

        RegistrationEmailRequest request = new RegistrationEmailRequest();
        request.setEmail(email);

        authService.requestRegistration(request);

        ArgumentCaptor<RegistrationToken> tokenCaptor = ArgumentCaptor.forClass(RegistrationToken.class);
        verify(registrationTokenRepository).save(tokenCaptor.capture());

        ArgumentCaptor<RegistrationRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(RegistrationRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        RegistrationToken savedToken = tokenCaptor.getValue();
        String rawToken = eventCaptor.getValue().token();

        assertNotNull(savedToken.getTokenHash());
        assertNotEquals(rawToken, savedToken.getTokenHash());
        assertEquals(sha256(rawToken), savedToken.getTokenHash());
        assertEquals(email, savedToken.getEmail());
        assertFalse(savedToken.isUsed());
    }

    @Test
    void duplicateEmailDoesNotCreateToken() {
        String email = "existing@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        RegistrationEmailRequest request = new RegistrationEmailRequest();
        request.setEmail(email);

        String result = authService.requestRegistration(request);

        assertEquals("The email already exists", result);
        verify(registrationTokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void newRequestInvalidatesPreviousToken() {
        String email = "user@example.com";
        RegistrationToken previousToken = new RegistrationToken();
        previousToken.setEmail(email);
        previousToken.setUsed(false);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(registrationTokenRepository.findAllByEmailAndUsedFalse(email))
                .thenReturn(List.of(previousToken));

        RegistrationEmailRequest request = new RegistrationEmailRequest();
        request.setEmail(email);

        authService.requestRegistration(request);

        assertTrue(previousToken.isUsed());
        verify(registrationTokenRepository).saveAll(List.of(previousToken));
    }

    @Test
    void validTokenCompletesRegistration() {
        String rawToken = "raw-token-value";
        String email = "newuser@example.com";

        RegistrationToken registrationToken = new RegistrationToken();
        registrationToken.setEmail(email);
        registrationToken.setTokenHash(sha256(rawToken));
        registrationToken.setUsed(false);
        registrationToken.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));

        CompleteRegistrationRequest request = new CompleteRegistrationRequest();
        request.setToken(rawToken);
        request.setName("New User");
        request.setPassword("password123");
        request.setPhoneNumber("1234567890");

        User mappedUser = new User();
        User savedUser = new User();
        savedUser.setUserId("user-id-1");
        savedUser.setEmail(email);

        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setUserId("user-id-1");
        expectedResponse.setEmail(email);

        when(registrationTokenRepository.findByTokenHash(sha256(rawToken)))
                .thenReturn(Optional.of(registrationToken));
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(mappedUser)).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(expectedResponse);

        UserResponse result = authService.completeRegistration(request);

        assertSame(expectedResponse, result);
        assertTrue(registrationToken.isUsed());
        verify(registrationTokenRepository).save(registrationToken);
        assertEquals(email, mappedUser.getEmail());
        assertEquals("encoded-password", mappedUser.getPasswordHash());
        assertEquals(Role.USER, mappedUser.getRole());
        assertEquals(UserStatus.ACTIVE, mappedUser.getStatus());
        assertTrue(mappedUser.isEnabled());
    }

    @Test
    void expiredTokenIsRejected() {
        String rawToken = "expired-token";
        RegistrationToken registrationToken = new RegistrationToken();
        registrationToken.setEmail("user@example.com");
        registrationToken.setTokenHash(sha256(rawToken));
        registrationToken.setUsed(false);
        registrationToken.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));

        when(registrationTokenRepository.findByTokenHash(sha256(rawToken)))
                .thenReturn(Optional.of(registrationToken));

        CompleteRegistrationRequest request = new CompleteRegistrationRequest();
        request.setToken(rawToken);
        request.setName("User");
        request.setPassword("password123");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> authService.completeRegistration(request));

        assertEquals("This registration link has expired", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void usedTokenIsRejected() {
        String rawToken = "used-token";
        RegistrationToken registrationToken = new RegistrationToken();
        registrationToken.setEmail("user@example.com");
        registrationToken.setTokenHash(sha256(rawToken));
        registrationToken.setUsed(true);
        registrationToken.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));

        when(registrationTokenRepository.findByTokenHash(sha256(rawToken)))
                .thenReturn(Optional.of(registrationToken));

        CompleteRegistrationRequest request = new CompleteRegistrationRequest();
        request.setToken(rawToken);
        request.setName("User");
        request.setPassword("password123");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> authService.completeRegistration(request));

        assertEquals("This registration link has already been used", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void passwordIsEncoded() {
        String rawToken = "raw-token-two";
        String email = "encode@example.com";

        RegistrationToken registrationToken = new RegistrationToken();
        registrationToken.setEmail(email);
        registrationToken.setTokenHash(sha256(rawToken));
        registrationToken.setUsed(false);
        registrationToken.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));

        CompleteRegistrationRequest request = new CompleteRegistrationRequest();
        request.setToken(rawToken);
        request.setName("Encoded User");
        request.setPassword("plainPassword1");

        User mappedUser = new User();
        User savedUser = new User();
        savedUser.setEmail(email);

        when(registrationTokenRepository.findByTokenHash(sha256(rawToken)))
                .thenReturn(Optional.of(registrationToken));
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode("plainPassword1")).thenReturn("hashed-plain-password");
        when(userRepository.save(mappedUser)).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(new UserResponse());

        authService.completeRegistration(request);

        verify(passwordEncoder).encode("plainPassword1");
        assertEquals("hashed-plain-password", mappedUser.getPasswordHash());
        assertNotEquals("plainPassword1", mappedUser.getPasswordHash());
    }

    @Test
    void correctCredentialsReturnJwt() {
        String email = "login@example.com";
        String rawPassword = "correctPassword1";

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hashed-password");
        user.setEnabled(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(Role.USER);

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(rawPassword);

        UserResponse userResponse = new UserResponse();
        userResponse.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("signed-jwt-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(3600L);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        AuthResponse response = authService.login(request);

        assertEquals("signed-jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());
        assertSame(userResponse, response.user());
    }

    @Test
    void incorrectPasswordIsRejected() {
        String email = "login@example.com";

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hashed-password");

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("wrongPassword");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashed-password")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.login(request));

        assertEquals("Invalid email or password", exception.getMessage());
        verify(jwtService, never()).generateToken(any());
    }
}