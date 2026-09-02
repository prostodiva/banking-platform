package com.bankapp.shared.domain;

/**
 * The caller is not who they need to be. Contexts subclass this so
 * GlobalExceptionHandler can map it without importing anything of theirs — same
 * shape as EntityNotFoundException and UnprocessableRequestException.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
