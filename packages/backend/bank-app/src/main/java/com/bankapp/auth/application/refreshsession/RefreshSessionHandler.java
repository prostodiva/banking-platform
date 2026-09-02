package com.bankapp.auth.application.refreshsession;

import com.bankapp.auth.application.AuthTokens;
import com.bankapp.auth.application.port.AccessTokenIssuer;
import com.bankapp.auth.application.port.DomainEventPublisher;
import com.bankapp.auth.application.port.IssuedToken;
import com.bankapp.auth.application.port.RefreshTokenIssuer;
import com.bankapp.auth.domain.RefreshToken;
import com.bankapp.auth.domain.RefreshTokenRepository;
import com.bankapp.auth.domain.RefreshTokenValue;
import com.bankapp.auth.domain.User;
import com.bankapp.auth.domain.UserRepository;
import com.bankapp.auth.domain.events.RefreshTokenReuseDetected;
import com.bankapp.auth.domain.exceptions.InvalidRefreshTokenException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshSessionHandler {

    private final RefreshTokenRepository refreshTokens;
    private final UserRepository users;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final DomainEventPublisher events;

    public RefreshSessionHandler(
        RefreshTokenRepository refreshTokens,
        UserRepository users,
        AccessTokenIssuer accessTokenIssuer,
        RefreshTokenIssuer refreshTokenIssuer,
        DomainEventPublisher events
    ) {
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenIssuer = refreshTokenIssuer;
        this.events = events;
    }

    /**
     * {@code noRollbackFor} is load-bearing. The reuse branch writes rows — it
     * revokes every active token — and then throws. A {@code RuntimeException}
     * escaping a {@code @Transactional} method marks the transaction rollback-only
     * by default, so every one of those UPDATEs would be discarded, and the event
     * publisher holds events until commit (docs/06) so
     * {@link RefreshTokenReuseDetected} would never be delivered either.
     *
     * <p>The endpoint would still answer 401 and the stolen token would still be
     * refused this time. From outside the process it looks exactly like success,
     * while the entire theft response quietly did not happen.
     *
     * <p>Safe on the other two 401 paths: they write nothing, so they commit an
     * empty transaction.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthTokens handle(RefreshSessionCommand command) {
        Instant now = Instant.now();
        String presentedHash = new RefreshTokenValue(command.refreshToken()).hash();

        RefreshToken presented = refreshTokens
            .findByTokenHash(presentedHash)
            .orElseThrow(InvalidRefreshTokenException::new);

        if (presented.isRevoked()) {
            // Either the real client is replaying or a thief holds a copy, and
            // nothing here can tell them apart. Assume the worse one.
            revokeEverySessionOf(presented.getUserId(), now);
            events.publish(new RefreshTokenReuseDetected(presented.getUserId(), now));
            throw new InvalidRefreshTokenException();
        }

        if (!presented.isUsableAt(now)) {
            throw new InvalidRefreshTokenException();
        }

        presented.revoke(now);
        refreshTokens.save(presented);

        // The only source of a role in this method. A refresh token is 256 random
        // bits with no claims, so there is nothing to carry over even if someone
        // wanted to — a demoted admin loses admin at their next refresh whether or
        // not anybody remembered to think about it.
        User user = users
            .findById(presented.getUserId())
            .orElseThrow(InvalidRefreshTokenException::new);

        IssuedToken access = accessTokenIssuer.issue(user.getId(), user.getRole());
        IssuedToken refresh = refreshTokenIssuer.issue(user.getId());

        return new AuthTokens(
            user.getId(),
            access.value(),
            refresh.value(),
            access.expiresAt()
        );
    }

    private void revokeEverySessionOf(UUID userId, Instant at) {
        for (RefreshToken token : refreshTokens.findActiveByUserId(userId)) {
            token.revoke(at);
            refreshTokens.save(token);
        }
    }
}
