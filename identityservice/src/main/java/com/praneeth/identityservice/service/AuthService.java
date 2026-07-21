package com.praneeth.identityservice.service;

import com.praneeth.identityservice.dto.*;
import com.praneeth.identityservice.entity.RegistrationToken;
import com.praneeth.identityservice.entity.Role;
import com.praneeth.identityservice.entity.User;
import com.praneeth.identityservice.entity.UserStatus;
import com.praneeth.identityservice.event.RegistrationRequestedEvent;
import com.praneeth.identityservice.mapper.UserMapper;
import com.praneeth.identityservice.repository.RegistrationTokenRepository;
import com.praneeth.identityservice.repository.UserRepository;
import com.praneeth.identityservice.security.JwtService;
import com.praneeth.identityservice.totp.TotpService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RegistrationTokenRepository registrationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final TotpService totpService;

    public AuthService(
            UserRepository userRepository,
            RegistrationTokenRepository registrationTokenRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher,
            UserMapper userMapper,
            JwtService jwtService,
            TotpService totpService
    ) {
        this.userRepository = userRepository;
        this.registrationTokenRepository = registrationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
        this.totpService = totpService;
    }

    @Transactional
    public String requestRegistration(RegistrationEmailRequest registrationEmailRequest) {
        String email = registrationEmailRequest.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            return "The email already exists";
        }

        List<RegistrationToken> existingTokens = registrationTokenRepository.findAllByEmailAndUsedFalse(email);
        existingTokens.forEach(token -> token.setUsed(true));
        registrationTokenRepository.saveAll(existingTokens);

        String rawToken = UUID.randomUUID().toString();
        RegistrationToken registrationToken = new RegistrationToken();
        registrationToken.setEmail(email);
        registrationToken.setTokenHash(hashToken(rawToken));
        registrationToken.setCreatedAt(Instant.now());
        registrationToken.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        registrationToken.setUsed(false);

        registrationTokenRepository.save(registrationToken);
        eventPublisher.publishEvent(new RegistrationRequestedEvent(email, rawToken));

        return "Registration request accepted, check your email";
    }

    @Transactional
    public UserResponse completeRegistration(CompleteRegistrationRequest request) {
        String submittedTokenHash = hashToken(request.getToken());
        RegistrationToken registrationToken = registrationTokenRepository.findByTokenHash(submittedTokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid registration token"));

        if (registrationToken.isUsed()) {
            throw new IllegalStateException("This registration link has already been used");
        }

        if (registrationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("This registration link has expired");
        }

        if (userRepository.existsByEmail(registrationToken.getEmail())) {
            throw new IllegalStateException("An account already exists for this email");
        }

        User user = userMapper.toEntity(request);
        user.setEmail(registrationToken.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        registrationToken.setUsed(true);
        registrationTokenRepository.save(registrationToken);

        return userMapper.toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        if (!passwordMatches) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new IllegalStateException("This account is disabled");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("This account is not active");
        }

        if (user.isTotpEnabled()) {
            String totpCode = request.getTotpCode();
            if (totpCode == null || totpCode.isBlank()) {
                return new AuthResponse(null, "Bearer", 0, userMapper.toResponse(user), true);
            }

            if (!totpService.verify(user.getTotpSecret(), totpCode)) {
                throw new IllegalArgumentException("Invalid two-factor authentication code");
            }
        }

        String accessToken = jwtService.generateToken(user);
        return new AuthResponse(
                accessToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                userMapper.toResponse(user)
        );
    }

    @Transactional(readOnly = true)
    public RegistrationTokenResponse validateRegistrationToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return new RegistrationTokenResponse(false, null, null, "Registration token is required");
        }

        RegistrationToken registrationToken = registrationTokenRepository.findByTokenHash(hashToken(rawToken)).orElse(null);
        if (registrationToken == null) {
            return new RegistrationTokenResponse(false, null, null, "Invalid registration link");
        }

        if (registrationToken.isUsed()) {
            return new RegistrationTokenResponse(false, null, registrationToken.getExpiresAt(), "This registration link has already been used");
        }

        if (registrationToken.getExpiresAt().isBefore(Instant.now())) {
            return new RegistrationTokenResponse(false, null, registrationToken.getExpiresAt(), "This registration link has expired");
        }

        return new RegistrationTokenResponse(true, registrationToken.getEmail(), registrationToken.getExpiresAt(), "Registration link is valid");
    }

    @Transactional
    public TotpSetupResponse setupTotp(String email) {
        User user = getUserByEmail(email);
        String secret = user.getTotpSecret();

        if (secret == null || secret.isBlank() || !user.isTotpEnabled()) {
            secret = totpService.generateSecret();
            user.setTotpSecret(secret);
            userRepository.save(user);
        }

        String otpAuthUri = totpService.buildOtpAuthUri(user.getEmail(), secret);
        return new TotpSetupResponse(secret, otpAuthUri, totpService.buildQrCodeDataUri(otpAuthUri));
    }

    @Transactional
    public TotpStatusResponse enableTotp(String email, TotpVerificationRequest request) {
        User user = getUserByEmail(email);

        if (user.getTotpSecret() == null || user.getTotpSecret().isBlank()) {
            throw new IllegalStateException("Set up two-factor authentication before enabling it");
        }

        if (!totpService.verify(user.getTotpSecret(), request.code())) {
            throw new IllegalArgumentException("Invalid two-factor authentication code");
        }

        user.setTotpEnabled(true);
        userRepository.save(user);
        return new TotpStatusResponse(true, "Two-factor authentication enabled");
    }

    @Transactional
    public TotpStatusResponse disableTotp(String email, TotpVerificationRequest request) {
        User user = getUserByEmail(email);

        if (!user.isTotpEnabled()) {
            return new TotpStatusResponse(false, "Two-factor authentication is already disabled");
        }

        if (!totpService.verify(user.getTotpSecret(), request.code())) {
            throw new IllegalArgumentException("Invalid two-factor authentication code");
        }

        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);
        return new TotpStatusResponse(false, "Two-factor authentication disabled");
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user was not found"));
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}