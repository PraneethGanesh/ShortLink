package com.praneeth.identityservice.service;

import com.praneeth.identityservice.dto.RegistrationEmailRequest;
import com.praneeth.identityservice.dto.UserResponse;
import com.praneeth.identityservice.entity.RegistrationToken;
import com.praneeth.identityservice.repository.RegistrationTokenRepository;
import com.praneeth.identityservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RegistrationTokenRepository registrationTokenRepository;
    public AuthService(UserRepository userRepository, RegistrationTokenRepository registrationTokenRepository) {
        this.userRepository = userRepository;
        this.registrationTokenRepository =  registrationTokenRepository;
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
        return "Check your Email for Registration";
    }
}
