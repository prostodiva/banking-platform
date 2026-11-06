
package com.bankapp.auth.api.dto;

import com.bankapp.auth.application.AuthTokens;
import java.time.Instant;
import java.util.UUID;

public record AuthResponse(
    UUID userId,
    String accessToken,
    String refreshToken,
    String tokenType,
    Instant expiresAt
) {
    public static AuthResponse from(AuthTokens tokens) {
        return new AuthResponse(
            tokens.userId(),
            tokens.accessToken(),
            tokens.refreshToken(),
            "Bearer",
            tokens.expiresAt()
        );
    }
}
