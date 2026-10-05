package com.collegelibrary.exception;

/** 401 — credentials are missing or wrong. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
