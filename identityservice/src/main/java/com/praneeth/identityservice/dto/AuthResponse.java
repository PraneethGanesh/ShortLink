package com.praneeth.identityservice.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user,
        boolean twoFactorRequired
) {
    public AuthResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            UserResponse user
    ) {
        this(accessToken, tokenType, expiresIn, user, false);
    }
}