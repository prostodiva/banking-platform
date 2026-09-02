package com.bankapp.auth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);

    /**
     * Takes the hash, a {@code String} — not a {@link RefreshTokenValue}. The
     * hashing happens in the handler, so there is no signature anywhere in the
     * persistence layer that could accidentally be handed a plaintext token.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findActiveByUserId(UUID userId);
}
