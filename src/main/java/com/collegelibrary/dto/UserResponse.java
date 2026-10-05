package com.collegelibrary.dto;

import com.collegelibrary.entity.User;
import java.time.LocalDateTime;

/** Safe view of a user — the password hash is NEVER included. */
public record UserResponse(
        Long id,
        String collegeId,
        String name,
        String email,
        String role,
        String course,
        Integer year,
        boolean active,
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getCollegeId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getCourse(),
                user.getYear(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
