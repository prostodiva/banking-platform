
package com.bankapp.auth.infrastructure.persistence;

import com.bankapp.auth.domain.RefreshToken;
import com.bankapp.auth.domain.RefreshTokenRepository;
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
}
