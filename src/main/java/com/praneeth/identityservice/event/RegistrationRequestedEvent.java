package com.praneeth.identityservice.event;

public record RegistrationRequestedEvent(
        String email,
        String token
) {
}
