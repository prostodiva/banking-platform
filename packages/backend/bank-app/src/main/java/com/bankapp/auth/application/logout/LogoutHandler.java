package com.bankapp.auth.application.logout;

import com.bankapp.auth.domain.RefreshTokenRepository;
import com.bankapp.auth.domain.RefreshTokenValue;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Not Spring Security's {@code
 * org.springframework.security.web.authentication.logout.LogoutHandler}. Different
 * package, implements nothing of theirs, so no filter picks this up — but an IDE
 * that auto-imports the other one produces a confusing compile error. Same
 * situation as {@code BadCredentialsException} in docs/29.
 */
@Service
public class LogoutHandler {

    private final RefreshTokenRepository refreshTokens;

    public LogoutHandler(RefreshTokenRepository refreshTokens) {
        this.refreshTokens = refreshTokens;
    }

    /**
     * Four outcomes, one response. Unknown token, already-revoked token and
     * somebody else's token all fall out of this chain doing nothing, because
     * every distinction drawn here is a fact handed to a caller who may not be
     * entitled to it.
     *
     * <p>No {@code if}, no {@code else}, no {@code throw}: {@code filter} +
     * {@code ifPresent} collapses "not there" and "not yours" into the same empty,
     * so the caller cannot tell which happened because the code itself stopped
     * distinguishing them. {@code revoke(Instant)} is a no-op on an
     * already-revoked token (docs/28 §2g), which covers the third case.
     */
    @Transactional
    public void handle(LogoutCommand command) {
        String presentedHash = new RefreshTokenValue(command.refreshToken()).hash();

        refreshTokens
            .findByTokenHash(presentedHash)
            .filter(token -> token.belongsTo(command.userId()))
            .ifPresent(token -> {
                token.revoke(Instant.now());
                refreshTokens.save(token);
            });
    }
}
