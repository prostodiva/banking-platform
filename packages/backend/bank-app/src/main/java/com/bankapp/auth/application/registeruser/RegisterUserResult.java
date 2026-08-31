
package com.bankapp.auth.application.registeruser;

import java.time.Instant;
import java.util.UUID;

public record RegisterUserResult(
    UUID userId,
    String accessToken,
    String refreshToken,
    Instant expiresAt
) {}
