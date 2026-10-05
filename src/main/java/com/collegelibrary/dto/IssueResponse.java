package com.collegelibrary.dto;

import com.collegelibrary.entity.BookIssue;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Borrowing record with the names already filled in (no lazy loading needed). */
public record IssueResponse(
        Long id,
        Long bookId,
        String bookTitle,
        String bookIsbn,
        Long userId,
        String userName,
        String userCollegeId,
        LocalDateTime issuedAt,
        LocalDate dueDate,
        LocalDateTime returnedAt,
        String status,
        boolean overdue
) {

    public static IssueResponse from(BookIssue issue) {
        boolean overdue = issue.getStatus() == com.collegelibrary.enums.IssueStatus.ISSUED
                && issue.getDueDate().isBefore(LocalDate.now());

        return new IssueResponse(
                issue.getId(),
                issue.getBook().getId(),
                issue.getBook().getTitle(),
                issue.getBook().getIsbn(),
                issue.getUser().getId(),
                issue.getUser().getName(),
                issue.getUser().getCollegeId(),
                issue.getIssuedAt(),
                issue.getDueDate(),
                issue.getReturnedAt(),
                issue.getStatus().name(),
                overdue
        );
    }
}
