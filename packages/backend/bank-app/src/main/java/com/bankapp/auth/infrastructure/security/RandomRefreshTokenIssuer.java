
package com.bankapp.auth.infrastructure.security;

import com.bankapp.auth.application.port.IssuedToken;
import com.bankapp.auth.application.port.RefreshTokenIssuer;
import com.bankapp.auth.domain.RefreshToken;
import com.bankapp.auth.domain.RefreshTokenRepository;
import com.bankapp.auth.domain.RefreshTokenValue;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class RandomRefreshTokenIssuer implements RefreshTokenIssuer {

    private final RefreshTokenRepository refreshTokens;
    private final AuthProperties properties;

    RandomRefreshTokenIssuer(RefreshTokenRepository refreshTokens, AuthProperties properties) {
        this.refreshTokens = refreshTokens;
        this.properties = properties;
    }

    @Override
    public IssuedToken issue(UUID userId) {
        RefreshTokenValue value = RefreshTokenValue.generate();
        Instant expiresAt = Instant.now().plus(properties.refreshTokenTtl());

        // Only the hash is persisted. The plaintext leaves in this response and
        // is never recoverable from the database.
        refreshTokens.save(RefreshToken.issue(userId, value, expiresAt));

        return new IssuedToken(value.value(), expiresAt);
    }
}
