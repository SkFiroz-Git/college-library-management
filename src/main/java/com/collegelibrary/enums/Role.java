package com.collegelibrary.enums;

/**
 * The three roles of the college library system.
 * The role is stored in the database and read by the backend —
 * the frontend never decides it.
 */
public enum Role {
    ADMIN,
    LIBRARIAN,
    STUDENT
}
