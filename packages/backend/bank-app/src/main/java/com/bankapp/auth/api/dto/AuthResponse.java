
package com.bankapp.auth.api.dto;

import com.bankapp.auth.application.login.LoginResult;
import com.bankapp.auth.application.registeruser.RegisterUserResult;
import java.time.Instant;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Instant expiresAt
) {
    public static AuthResponse from(RegisterUserResult result) {
        return new AuthResponse(
            result.accessToken(),
            result.refreshToken(),
            "Bearer",
            result.expiresAt()
        );
    }

    public static AuthResponse from(LoginResult result) {
        return new AuthResponse(
            result.accessToken(),
            result.refreshToken(),
            "Bearer",
            result.expiresAt()
        );
    }
}
