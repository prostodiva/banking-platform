package com.bankapp.auth.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * An already-revoked refresh token was presented. Either the legitimate client
 * replayed a request or a thief holds a copy, and nothing can tell them apart —
 * so this is published on every occurrence and treated as the latter.
 *
 * <p>The happy path publishes nothing: a refresh is the same session continuing,
 * not a new business fact. This is the rare one, and the strongest signal the
 * auth context emits.
 */
public record RefreshTokenReuseDetected(UUID userId, Instant occurredAt) {}
