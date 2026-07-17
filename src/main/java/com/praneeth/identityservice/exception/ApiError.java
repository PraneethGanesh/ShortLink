package com.praneeth.identityservice.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors,
        Instant timestamp
) {
}