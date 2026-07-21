package com.praneeth.identityservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TotpVerificationRequest(
        @NotBlank(message = "TOTP code is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "TOTP code must be 6 digits")
        String code
) {
}