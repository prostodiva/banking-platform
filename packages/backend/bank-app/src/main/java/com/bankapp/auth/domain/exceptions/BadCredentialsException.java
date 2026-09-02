package com.bankapp.auth.domain.exceptions;

import com.bankapp.shared.domain.UnauthorizedException;

/**
 * One exception for both failure modes, on purpose. There is no
 * UserNotFoundException and no WrongPasswordException, because a type the code
 * can distinguish is a type someone will eventually map to two different
 * responses.
 */
public class BadCredentialsException extends UnauthorizedException {

    public BadCredentialsException() {
        super("Invalid email or password");
    }
}
