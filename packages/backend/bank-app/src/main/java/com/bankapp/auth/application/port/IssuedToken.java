
package com.bankapp.auth.application.port;

import java.time.Instant;

public record IssuedToken(String value, Instant expiresAt) {
    @Override
    public String toString() {
        return "IssuedToken[expiresAt=" + expiresAt + "]";
    }
}
