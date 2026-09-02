package com.bankapp.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, updatable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RefreshToken() {
    }

    private RefreshToken(UUID userId, String tokenHash, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public static RefreshToken issue(UUID userId, RefreshTokenValue value, Instant expiresAt) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (value == null) {
            throw new IllegalArgumentException("value is required");
        }
        return new RefreshToken(userId, value.hash(), expiresAt);
    }

    public void revoke(Instant at) {
        if (revokedAt == null) {
            this.revokedAt = at;
        }
    }

    public boolean isUsableAt(Instant moment) {
        return revokedAt == null && moment.isBefore(expiresAt);
    }

    /**
     * A predicate, not {@code getRevokedAt()}. Revoked and expired take different
     * paths in {@code RefreshSessionHandler} — one is a theft signal, the other is
     * ordinary — and {@link #isUsableAt} collapses both into one boolean. The
     * caller needs the answer, not the timestamp.
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Ask the object rather than exposing the hash. The only question anyone has
     * of a stored hash is "is this the one?", and a getter invites answering it
     * with {@code ==} or with a comparison that leaks length.
     */
    public boolean matchesHash(String candidate) {
        return tokenHash.equals(candidate);
    }

    /**
     * A predicate rather than comparing {@code getUserId()} in the handler.
     * Ownership is a question about the token, so the token answers it — and there
     * is then exactly one place to look when someone asks how logout decides.
     */
    public boolean belongsTo(UUID candidate) {
        return userId.equals(candidate);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }
}
