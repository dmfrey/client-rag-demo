package com.example.clientragdemo.shared.exception;

/**
 * Extend this from a feature's domain exceptions to get a 409 response for free from the global
 * exception handler, without that handler needing to know about the feature-specific type.
 */
public abstract class ConflictException extends RuntimeException {

    protected ConflictException(String message) {
        super(message);
    }
}
