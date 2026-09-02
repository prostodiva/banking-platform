package com.bankapp.auth.application.refreshsession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bankapp.auth.application.AuthTokens;
import com.bankapp.auth.application.port.IssuedToken;
import com.bankapp.auth.domain.Email;
import com.bankapp.auth.domain.PasswordHash;
import com.bankapp.auth.domain.RefreshToken;
import com.bankapp.auth.domain.RefreshTokenRepository;
import com.bankapp.auth.domain.RefreshTokenValue;
import com.bankapp.auth.domain.Role;
import com.bankapp.auth.domain.User;
import com.bankapp.auth.domain.UserRepository;
import com.bankapp.auth.domain.events.RefreshTokenReuseDetected;
import com.bankapp.auth.domain.exceptions.InvalidRefreshTokenException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshSessionHandlerTest {

    private static final User USER =
        User.register(new Email("ann@example.com"), "Ann Lee", new PasswordHash("stored"));

    private final List<RefreshToken> stored = new ArrayList<>();
    private final List<Object> published = new ArrayList<>();
    private final List<Role> rolesIssued = new ArrayList<>();

    private final RefreshTokenRepository refreshTokens = new RefreshTokenRepository() {
        @Override
        public RefreshToken save(RefreshToken token) {
            if (!stored.contains(token)) {
                stored.add(token);
            }
            return token;
        }

        @Override
        public Optional<RefreshToken> findByTokenHash(String tokenHash) {
            return stored.stream()
                .filter(t -> t.matchesHash(tokenHash))
                .findFirst();
        }

        @Override
        public List<RefreshToken> findActiveByUserId(UUID userId) {
            return stored.stream()
                .filter(t -> t.getUserId().equals(userId) && !t.isRevoked())
                .toList();
        }
    };

    private final UserRepository users = new UserRepository() {
        @Override
        public User save(User user) {
            throw new UnsupportedOperationException("refresh must not write users");
        }

        @Override
        public Optional<User> findByEmail(Email email) {
            throw new UnsupportedOperationException("refresh looks up by id, not email");
        }

        @Override
        public Optional<User> findById(UUID id) {
            return id.equals(USER.getId()) ? Optional.of(USER) : Optional.empty();
        }
    };

    private RefreshSessionHandler handler() {
        return new RefreshSessionHandler(
            refreshTokens,
            users,
            (userId, role) -> {
                rolesIssued.add(role);
                return new IssuedToken("access-token", Instant.MAX);
            },
            userId -> new IssuedToken("new-refresh-token", Instant.MAX),
            published::add
        );
    }

    private RefreshTokenValue storeTokenExpiringIn(long days) {
        RefreshTokenValue value = RefreshTokenValue.generate();
        stored.add(RefreshToken.issue(
            USER.getId(),
            value,
            Instant.now().plus(days, ChronoUnit.DAYS)
        ));
        return value;
    }

    @Test
    void rotatesTheTokenAndIssuesANewPair() {
        RefreshTokenValue presented = storeTokenExpiringIn(14);

        AuthTokens result = handler().handle(new RefreshSessionCommand(presented.value()));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(stored.getFirst().isRevoked()).isTrue();
        assertThat(published).isEmpty();
    }

    @Test
    void takesTheRoleFromTheUserRowNotTheToken() {
        RefreshTokenValue presented = storeTokenExpiringIn(14);

        handler().handle(new RefreshSessionCommand(presented.value()));

        // The refresh token is opaque and carries no claims, so this can only
        // have come from users.findById. There is no ADMIN case to test because
        // User.register cannot produce one — ADR-004 decision 5 working as built.
        assertThat(rolesIssued).containsExactly(Role.CUSTOMER);
    }

    @Test
    void refusesAnUnknownToken() {
        assertThatThrownBy(() ->
            handler().handle(new RefreshSessionCommand(RefreshTokenValue.generate().value()))
        ).isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(published).isEmpty();
    }

    @Test
    void refusesAnExpiredToken() {
        RefreshTokenValue presented = storeTokenExpiringIn(-1);

        assertThatThrownBy(() ->
            handler().handle(new RefreshSessionCommand(presented.value()))
        ).isInstanceOf(InvalidRefreshTokenException.class);

        // Expiry is not theft. Other sessions survive and nothing is published.
        assertThat(published).isEmpty();
    }

    @Test
    void reusingARevokedTokenKillsEverySessionAndPublishesOnce() {
        RefreshTokenValue presented = storeTokenExpiringIn(14);
        RefreshTokenValue otherDevice = storeTokenExpiringIn(14);

        handler().handle(new RefreshSessionCommand(presented.value()));

        assertThatThrownBy(() ->
            handler().handle(new RefreshSessionCommand(presented.value()))
        ).isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(stored).allSatisfy(token -> assertThat(token.isRevoked()).isTrue());
        assertThat(published)
            .singleElement()
            .isInstanceOf(RefreshTokenReuseDetected.class);

        // and the other device really is dead
        assertThatThrownBy(() ->
            handler().handle(new RefreshSessionCommand(otherDevice.value()))
        ).isInstanceOf(InvalidRefreshTokenException.class);
    }
}
