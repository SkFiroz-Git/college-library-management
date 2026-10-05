package com.collegelibrary.dto;

import com.collegelibrary.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin creates a new account (student / librarian / admin). */
public record UserCreateRequest(
        @NotBlank(message = "College ID is required")
        @Size(max = 40, message = "College ID must be at most 40 characters")
        String collegeId,

        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must be at most 120 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Please enter a valid email address")
        @Size(max = 160, message = "Email must be at most 160 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,

        @NotNull(message = "Role is required")
        Role role,

        String course,

        Integer year
) {}
