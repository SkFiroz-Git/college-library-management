package com.collegelibrary.dto;

import com.collegelibrary.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Admin edits an existing account.
 * Password is optional: leave it null/empty to keep the old one.
 */
public record UserUpdateRequest(
        @NotBlank(message = "College ID is required")
        @Size(max = 40)
        String collegeId,

        @NotBlank(message = "Name is required")
        @Size(max = 120)
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Please enter a valid email address")
        @Size(max = 160)
        String email,

        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,

        @NotNull(message = "Role is required")
        Role role,

        String course,

        Integer year,

        @NotNull(message = "Active status is required")
        Boolean active
) {}
