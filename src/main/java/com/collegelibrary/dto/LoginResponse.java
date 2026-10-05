package com.collegelibrary.dto;

/** What the server returns after a successful login. */
public record LoginResponse(
        Long id,
        String name,
        String email,
        String collegeId,
        String role
) {}
