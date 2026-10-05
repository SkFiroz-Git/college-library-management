package com.collegelibrary.dto;

/** Numbers shown on the admin dashboard. */
public record DashboardStats(
        long totalUsers,
        long admins,
        long librarians,
        long students,
        long activeUsers,
        long totalBooks,
        long activeBooks,
        long totalCopies,
        long availableCopies,
        long issuedNow,
        long overdue,
        long returnedAllTime,
        java.util.List<IssueResponse> recentIssues
) {}
