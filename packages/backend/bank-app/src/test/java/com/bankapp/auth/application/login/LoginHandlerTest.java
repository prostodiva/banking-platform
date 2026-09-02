package com.bankapp.auth.application.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bankapp.auth.application.AuthTokens;
import com.bankapp.auth.application.port.IssuedToken;
import com.bankapp.auth.application.port.PasswordHasher;
import com.bankapp.auth.domain.Email;
import com.bankapp.auth.domain.PasswordHash;
import com.bankapp.auth.domain.Role;
import com.bankapp.auth.domain.User;
import com.bankapp.auth.domain.UserRepository;
import com.bankapp.auth.domain.events.UserLoggedIn;
import com.bankapp.auth.domain.exceptions.BadCredentialsException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoginHandlerTest {

    private static final PasswordHash STORED = new PasswordHash("stored-hash");
    private static final PasswordHash DUMMY = new PasswordHash("dummy-hash");

    private final List<PasswordHash> verified = new ArrayList<>();
    private final List<Object> published = new ArrayList<>();

    /** Records every hash it was asked to verify, and matches only the stored one. */
    private final PasswordHasher passwords = new PasswordHasher() {
        @Override
        public PasswordHash hash(String plaintext) {
            throw new UnsupportedOperationException("login must not hash");
        }

        @Override
        public boolean matches(String plaintext, PasswordHash hash) {
            verified.add(hash);
            return hash.equals(STORED) && plaintext.equals("right password");
        }

        @Override
        public PasswordHash dummyHash() {
            return DUMMY;
        }
    };

    private LoginHandler handlerFor(Optional<User> user) {
        UserRepository users = new UserRepository() {
            @Override
            public User save(User u) {
                throw new UnsupportedOperationException("login must not write users");
            }

            @Override
            public Optional<User> findByEmail(Email email) {
                return user;
            }

            @Override
            public Optional<User> findById(UUID id) {
                throw new UnsupportedOperationException("login looks up by email, not id");
            }
        };

        return new LoginHandler(
            users,
            passwords,
            (userId, role) -> new IssuedToken("access-for-" + role, Instant.MAX),
            userId -> new IssuedToken("refresh-token", Instant.MAX),
            published::add
        );
    }

    private static User existingUser() {
        return User.register(new Email("ann@example.com"), "Ann Lee", STORED);
    }

    @Test
    void issuesBothTokensOnCorrectCredentials() {
        User user = existingUser();

        AuthTokens result = handlerFor(Optional.of(user))
            .handle(new LoginCommand("Ann@Example.com", "right password"));

        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.accessToken()).isEqualTo("access-for-" + Role.CUSTOMER);
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(published).singleElement().isInstanceOf(UserLoggedIn.class);
    }

    @Test
    void refusesAWrongPassword() {
        assertThatThrownBy(() ->
            handlerFor(Optional.of(existingUser()))
                .handle(new LoginCommand("ann@example.com", "wrong password"))
        ).isInstanceOf(BadCredentialsException.class);

        assertThat(published).isEmpty();
    }

    @Test
    void refusesAnUnknownEmail() {
        assertThatThrownBy(() ->
            handlerFor(Optional.empty())
                .handle(new LoginCommand("nobody@example.com", "right password"))
        ).isInstanceOf(BadCredentialsException.class);

        assertThat(published).isEmpty();
    }

    /**
     * The timing defence. Over HTTP this is only observable as a difference in
     * latency, which makes for a flaky test; here it is one assertion.
     */
    @Test
    void verifiesAPasswordEvenWhenTheEmailIsUnknown() {
        assertThatThrownBy(() ->
            handlerFor(Optional.empty())
                .handle(new LoginCommand("nobody@example.com", "right password"))
        ).isInstanceOf(BadCredentialsException.class);

        assertThat(verified).containsExactly(DUMMY);
    }

    @Test
    void bothFailuresThrowTheSameTypeWithTheSameMessage() {
        Throwable unknownEmail = catchOf(Optional.empty(), "right password");
        Throwable wrongPassword = catchOf(Optional.of(existingUser()), "wrong password");

        assertThat(unknownEmail).hasSameClassAs(wrongPassword);
        assertThat(unknownEmail).hasMessage(wrongPassword.getMessage());
    }

    private Throwable catchOf(Optional<User> user, String password) {
        try {
            handlerFor(user).handle(new LoginCommand("ann@example.com", password));
            throw new AssertionError("expected BadCredentialsException");
        } catch (BadCredentialsException e) {
            return e;
        }
    }
}
