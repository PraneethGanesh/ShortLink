package com.praneeth.identityservice.service;

import com.praneeth.identityservice.dto.CompleteRegistrationRequest;
import com.praneeth.identityservice.dto.RegistrationEmailRequest;
import com.praneeth.identityservice.entity.RegistrationToken;
import com.praneeth.identityservice.entity.Role;
import com.praneeth.identityservice.entity.User;
import com.praneeth.identityservice.entity.UserStatus;
import com.praneeth.identityservice.event.RegistrationRequestedEvent;
import com.praneeth.identityservice.repository.RegistrationTokenRepository;
import com.praneeth.identityservice.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RegistrationTokenRepository registrationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    public AuthService(UserRepository userRepository, RegistrationTokenRepository registrationTokenRepository
    , PasswordEncoder passwordEncoder, ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.registrationTokenRepository =  registrationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    public String requestRegistration(RegistrationEmailRequest registrationEmailRequest) {

        String email = registrationEmailRequest.getEmail().trim().toLowerCase(Locale.ROOT);
        if(userRepository.existsByEmail(email)) {
            return "The email already exists";
        }
        String rawToken = UUID.randomUUID().toString();

        RegistrationToken registrationToken = new RegistrationToken();
        registrationToken.setEmail(email);
        registrationToken.setTokenHash(rawToken);
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
    public String completeRegistration(
            CompleteRegistrationRequest request) {
        RegistrationToken registrationToken=registrationTokenRepository
                .findByTokenHash(request.getToken())
                .orElseThrow(()-> new RuntimeException("Token not found"));
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
        User user = new User();
        user.setName(request.getName());
        user.setEmail(registrationToken.getEmail());
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);

        userRepository.save(user);

        registrationToken.setUsed(true);
        registrationTokenRepository.save(registrationToken);
        return "Registration completed successfully, now move to login Page";
    }

}
