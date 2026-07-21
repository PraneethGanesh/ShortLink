package com.praneeth.identityservice.dto;

public record TotpSetupResponse(
        String secret,
        String otpAuthUri,
        String qrCodeDataUri
) {
}