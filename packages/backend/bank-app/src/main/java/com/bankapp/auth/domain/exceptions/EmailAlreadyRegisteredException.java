package com.bankapp.auth.domain.exceptions;

/**
 * Extends IllegalStateException so GlobalExceptionHandler's existing mapping
 * turns it into 409 with this message, with no new advice and no import of
 * anything auth-specific into shared/web.
 */
public class EmailAlreadyRegisteredException extends IllegalStateException {

    public EmailAlreadyRegisteredException() {
        super("An account with this email already exists");
    }
}
