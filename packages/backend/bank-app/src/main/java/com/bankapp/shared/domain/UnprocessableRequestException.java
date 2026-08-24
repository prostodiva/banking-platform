package com.bankapp.shared.domain;

/**
 * The request was understood and is well formed, but refusing it is the correct
 * answer — 422, not 400. Contexts subclass this so GlobalExceptionHandler can map
 * it without importing anything of theirs (same shape as EntityNotFoundException).
 */
public class UnprocessableRequestException extends RuntimeException {

    public UnprocessableRequestException(String message) {
        super(message);
    }
}
