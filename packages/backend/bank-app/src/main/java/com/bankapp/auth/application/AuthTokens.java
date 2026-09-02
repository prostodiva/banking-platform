package com.bankapp.auth.application;

import java.time.Instant;
import java.util.UUID;

/**
 * What all three authentication use cases return. Lives outside the use-case
 * folders because it belongs to none of them.
 */
public record AuthTokens(
    UUID userId,
    String accessToken,
    String refreshToken,
    Instant expiresAt
) {}
