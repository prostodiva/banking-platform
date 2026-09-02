package com.bankapp.auth.application.logout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.bankapp.auth.domain.RefreshToken;
import com.bankapp.auth.domain.RefreshTokenRepository;
import com.bankapp.auth.domain.RefreshTokenValue;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LogoutHandlerTest {

    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID SOMEONE_ELSE = UUID.randomUUID();

    private final List<RefreshToken> stored = new ArrayList<>();

    private final RefreshTokenRepository refreshTokens = new RefreshTokenRepository() {
        @Override
        public RefreshToken save(RefreshToken token) {
            return token;
        }

        @Override
        public Optional<RefreshToken> findByTokenHash(String tokenHash) {
            return stored.stream().filter(t -> t.matchesHash(tokenHash)).findFirst();
        }

        @Override
        public List<RefreshToken> findActiveByUserId(UUID userId) {
            throw new UnsupportedOperationException("logout revokes one token, not a family");
        }
    };

    private final LogoutHandler handler = new LogoutHandler(refreshTokens);

    private RefreshTokenValue storeTokenFor(UUID userId) {
        RefreshTokenValue value = RefreshTokenValue.generate();
        RefreshToken token =
            RefreshToken.issue(userId, value, Instant.now().plus(14, ChronoUnit.DAYS));
        stored.add(token);
        return value;
    }

    @Test
    void revokesTheCallersOwnToken() {
        RefreshTokenValue token = storeTokenFor(OWNER);

        handler.handle(new LogoutCommand(OWNER, token.value()));

        assertThat(stored.getFirst().isRevoked()).isTrue();
    }

    @Test
    void ignoresAnUnknownToken() {
        assertThatCode(() ->
            handler.handle(new LogoutCommand(OWNER, RefreshTokenValue.generate().value()))
        ).doesNotThrowAnyException();
    }

    /**
     * The important one. It must return normally — a thrown exception with its own
     * status would tell the caller that the token they hold is real and belongs
     * to a real other account, which is a distinction they had no way to make
     * before asking.
     */
    @Test
    void ignoresATokenBelongingToSomeoneElse() {
        RefreshTokenValue token = storeTokenFor(SOMEONE_ELSE);

        assertThatCode(() -> handler.handle(new LogoutCommand(OWNER, token.value())))
            .doesNotThrowAnyException();

        assertThat(stored.getFirst().isRevoked()).isFalse();
    }

    @Test
    void isIdempotent() {
        RefreshTokenValue token = storeTokenFor(OWNER);

        handler.handle(new LogoutCommand(OWNER, token.value()));
        assertThatCode(() -> handler.handle(new LogoutCommand(OWNER, token.value())))
            .doesNotThrowAnyException();

        assertThat(stored.getFirst().isRevoked()).isTrue();
    }

    @Test
    void neverRevokesAFamily() {
        // findActiveByUserId throws in the fake above. If logout ever starts
        // calling it, this fails — logout is the one-device response, and reuse
        // detection is the theft response.
        RefreshTokenValue token = storeTokenFor(OWNER);

        assertThatCode(() -> handler.handle(new LogoutCommand(OWNER, token.value())))
            .doesNotThrowAnyException();
    }
}
