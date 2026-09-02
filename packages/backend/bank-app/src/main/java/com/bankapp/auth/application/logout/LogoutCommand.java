package com.bankapp.auth.application.logout;

import java.util.UUID;

/**
 * {@code userId} is a field on the command and the controller fills it from the
 * validated access token — never from the request body. This is the shape every
 * retrofitted use case takes in NFR-11: the application layer receives an
 * identity it can trust, and has no idea where HTTP put it.
 */
public record LogoutCommand(UUID userId, String refreshToken) {
    @Override
    public String toString() {
        return "LogoutCommand[userId=" + userId + "]";
    }
}
