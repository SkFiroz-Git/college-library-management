package com.collegelibrary.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * What the login form sends.
 * "identifier" = the college ID or the email (both are accepted).
 */
public record LoginRequest(
        @NotBlank(message = "College ID or email is required")
        String identifier,

        @NotBlank(message = "Password is required")
        String password
) {}
