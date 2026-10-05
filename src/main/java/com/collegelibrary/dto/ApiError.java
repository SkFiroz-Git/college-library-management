package com.collegelibrary.dto;

import java.time.Instant;
import java.util.Map;

/**
 * One error shape for every API failure, so the frontend only has
 * to understand a single format.
 */
public record ApiError(
        int status,
        String message,
        Map<String, String> fields,
        Instant timestamp
) {

    public ApiError(int status, String message) {
        this(status, message, null, Instant.now());
    }
}
