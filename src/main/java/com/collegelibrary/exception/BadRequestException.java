package com.collegelibrary.exception;

/** 400 — the request is understood but cannot be executed right now. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
