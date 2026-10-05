package com.collegelibrary.exception;

/** 409 — the request conflicts with existing data (duplicate email, ISBN, ...). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
