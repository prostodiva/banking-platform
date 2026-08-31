package com.bankapp.auth.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * The plaintext half of a refresh token: 256 bits of randomness that only ever
 * exists in a response body and in the client's storage. What we keep is
 * {@link #hash()}.
 */
public record RefreshTokenValue(String value) {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int BYTES = 32;

    public RefreshTokenValue {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("refresh token value is required");
        }
    }

    public static RefreshTokenValue generate() {
        byte[] bytes = new byte[BYTES];
        RANDOM.nextBytes(bytes);
        return new RefreshTokenValue(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    /**
     * SHA-256, not BCrypt. BCrypt's cost factor defends a low-entropy human
     * password against offline guessing; there is nothing to guess in 256 random
     * bits, so key stretching would only add ~100ms to every refresh.
     */
    public String hash() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every JVM", e);
        }
    }

    @Override
    public String toString() {
        return "RefreshTokenValue[REDACTED]";
    }
}
