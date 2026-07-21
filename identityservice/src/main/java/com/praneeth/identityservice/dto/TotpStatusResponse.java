package com.praneeth.identityservice.dto;

public record TotpStatusResponse(
        boolean enabled,
        String message
) {
}