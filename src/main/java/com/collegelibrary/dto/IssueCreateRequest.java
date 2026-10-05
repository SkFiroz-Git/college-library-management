package com.collegelibrary.dto;

import jakarta.validation.constraints.NotNull;

/** Librarian/admin lends a book to a user. */
public record IssueCreateRequest(
        @NotNull(message = "Book is required")
        Long bookId,

        @NotNull(message = "Member is required")
        Long userId
) {}
