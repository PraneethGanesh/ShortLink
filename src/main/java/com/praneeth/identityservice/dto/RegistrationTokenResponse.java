package com.praneeth.identityservice.dto;

import java.time.Instant;

public record RegistrationTokenResponse(
        boolean valid,
        String email,
        Instant expiresAt,
        String message
) {
}
