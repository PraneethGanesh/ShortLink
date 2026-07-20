package com.praneeth.urlservice.dto;

import java.time.Instant;

public record ShortUrlResponse(
        String id,
        String longUrl,
        String shortCode,
        String shortUrl,
        boolean enabled,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}