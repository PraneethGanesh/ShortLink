package com.praneeth.urlservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateShortUrlRequest(

        @NotBlank(message = "Long URL is required")
        @Size(
                max = 2048,
                message = "Long URL cannot exceed 2048 characters"
        )
        String longUrl,

        @Pattern(
                regexp = "^[a-zA-Z0-9_-]{4,32}$",
                message = "Custom alias must contain 4 to 32 letters, numbers, hyphens or underscores"
        )
        String customAlias,

        Instant expiresAt
) {
}