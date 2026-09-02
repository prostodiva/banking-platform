
package com.bankapp.auth.application.login;

import java.time.Instant;
import java.util.UUID;

public record LoginResult(
    UUID userId,
    String accessToken,
    String refreshToken,
    Instant expiresAt
) {}
