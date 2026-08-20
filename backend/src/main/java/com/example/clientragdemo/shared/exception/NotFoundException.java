package com.example.clientragdemo.shared.exception;

/**
 * Extend this from a feature's domain exceptions to get a 404 response for free from the global
 * exception handler, without that handler needing to know about the feature-specific type.
 */
public abstract class NotFoundException extends RuntimeException {

    protected NotFoundException(String message) {
        super(message);
    }
}
