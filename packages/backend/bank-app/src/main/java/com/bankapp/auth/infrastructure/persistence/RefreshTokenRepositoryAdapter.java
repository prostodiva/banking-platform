
package com.bankapp.auth.infrastructure.persistence;

import com.bankapp.auth.domain.RefreshToken;
import com.bankapp.auth.domain.RefreshTokenRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * Plain {@code save}, unlike {@link UserRepositoryAdapter}: there is no
     * constraint on this table whose violation means anything to a caller, so
     * there is nothing to translate and no reason to flush early.
     */
    @Override
    public RefreshToken save(RefreshToken token) {
        return jpa.save(token);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash);
    }

    /**
     * "Active" is spelled out in the derived query rather than filtered in Java:
     * the reuse path runs on a user who may have many rows, and only the unrevoked
     * ones are about to be updated.
     */
    @Override
    public List<RefreshToken> findActiveByUserId(UUID userId) {
        return jpa.findAllByUserIdAndRevokedAtIsNull(userId);
    }
}
