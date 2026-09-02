
package com.bankapp.auth.api.dto;

import com.bankapp.auth.application.AuthTokens;
import java.time.Instant;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Instant expiresAt
) {
    public static AuthResponse from(AuthTokens tokens) {
        return new AuthResponse(
            tokens.accessToken(),
            tokens.refreshToken(),
            "Bearer",
            tokens.expiresAt()
        );
    }
}
