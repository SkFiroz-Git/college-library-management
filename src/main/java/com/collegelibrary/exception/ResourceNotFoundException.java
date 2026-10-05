package com.collegelibrary.exception;

/** 404 — the requested record does not exist. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found with id " + id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
