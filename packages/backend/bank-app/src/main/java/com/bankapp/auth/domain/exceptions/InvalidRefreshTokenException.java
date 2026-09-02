package com.bankapp.auth.domain.exceptions;

import com.bankapp.shared.domain.UnauthorizedException;

/**
 * One exception for unknown, expired and reused, for the same reason login has
 * one for unknown-email and wrong-password: a type the code can distinguish is a
 * type someone eventually maps to three different responses.
 */
public class InvalidRefreshTokenException extends UnauthorizedException {

    public InvalidRefreshTokenException() {
        super("Invalid refresh token");
    }
}
