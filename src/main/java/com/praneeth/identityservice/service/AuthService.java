package com.praneeth.identityservice.service;

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
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RegistrationTokenRepository registrationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final UserMapper userMapper;
    public AuthService(UserRepository userRepository, RegistrationTokenRepository registrationTokenRepository
    , PasswordEncoder passwordEncoder, ApplicationEventPublisher eventPublisher,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.registrationTokenRepository =  registrationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.userMapper = userMapper;
    }

    public String requestRegistration(RegistrationEmailRequest registrationEmailRequest) {

        String email = registrationEmailRequest.getEmail().trim().toLowerCase(Locale.ROOT);
        if(userRepository.existsByEmail(email)) {
            return "The email already exists";
        }
        String rawToken = UUID.randomUUID().toString();

        RegistrationToken registrationToken = new RegistrationToken();
        registrationToken.setEmail(email);
        String tokenHash = hashToken(rawToken);
        registrationToken.setTokenHash(tokenHash);
        registrationToken.setCreatedAt(Instant.now());
        registrationToken.setExpiresAt(
                Instant.now().plus(15, ChronoUnit.MINUTES)
        );
        registrationToken.setUsed(false);

        registrationTokenRepository.save(registrationToken);
        eventPublisher.publishEvent(
                new RegistrationRequestedEvent(email, rawToken)
        );
        return "Registration request accepted, check your email";
    }
    @Transactional
    public UserResponse completeRegistration(
            CompleteRegistrationRequest request) {
        String submittedTokenHash = hashToken(request.getToken());

        RegistrationToken registrationToken =
                registrationTokenRepository
                        .findByTokenHash(submittedTokenHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid registration token"
                                )
                        );

        if (registrationToken.isUsed()) {
            throw new IllegalStateException(
                    "This registration link has already been used"
            );
        }

        if (registrationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException(
                    "This registration link has expired"
            );
        }

        if (userRepository.existsByEmail(registrationToken.getEmail())) {
            throw new IllegalStateException(
                    "An account already exists for this email"
            );
        }
        var user = userMapper.toEntity(request);

        user.setEmail(registrationToken.getEmail());
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);

        var savedUser = userRepository.save(user);

        registrationToken.setUsed(true);
        registrationTokenRepository.save(registrationToken);

        var userResponse = userMapper.toResponse(savedUser);

        return userResponse;
        }

    private String hashToken(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public String login(LoginRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid email or password"
                        )
                );

        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new IllegalArgumentException(
                    "Invalid email or password"
            );
        }

        if (!user.isEnabled()) {
            throw new IllegalStateException(
                    "This account is disabled"
            );
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException(
                    "This account is not active"
            );
        }

        return "Welcome to the ShortUrl application";
    }
}
