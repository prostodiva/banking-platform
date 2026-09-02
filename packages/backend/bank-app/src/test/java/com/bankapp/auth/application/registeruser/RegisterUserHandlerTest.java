package com.bankapp.auth.application.registeruser;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankapp.auth.application.port.IssuedToken;
import com.bankapp.auth.application.port.PasswordHasher;
import com.bankapp.auth.domain.Email;
import com.bankapp.auth.domain.PasswordHash;
import com.bankapp.auth.domain.Role;
import com.bankapp.auth.domain.User;
import com.bankapp.auth.domain.UserRepository;
import com.bankapp.auth.domain.events.UserRegistered;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RegisterUserHandlerTest {

    @Test
    void hashesThePasswordStoresTheUserAndIssuesBothTokens() {
        List<User> saved = new ArrayList<>();
        List<Object> published = new ArrayList<>();
        List<String> hashed = new ArrayList<>();

        UserRepository users = new UserRepository() {
            @Override
            public User save(User user) {
                saved.add(user);
                return user;
            }

            @Override
            public Optional<User> findByEmail(Email email) {
                throw new UnsupportedOperationException("register must not read users");
            }
        };

        PasswordHasher passwordHasher = new PasswordHasher() {
            @Override
            public PasswordHash hash(String plaintext) {
                hashed.add(plaintext);
                return new PasswordHash("hashed:" + plaintext);
            }

            @Override
            public boolean matches(String plaintext, PasswordHash hash) {
                throw new UnsupportedOperationException("register must not verify passwords");
            }

            @Override
            public PasswordHash dummyHash() {
                throw new UnsupportedOperationException("register must not verify passwords");
            }
        };

        RegisterUserHandler handler = new RegisterUserHandler(
            users,
            passwordHasher,
            (userId, role) -> new IssuedToken("access-for-" + role, Instant.MAX),
            userId -> new IssuedToken("refresh-token", Instant.MAX),
            published::add
        );

        RegisterUserResult result = handler.handle(
            new RegisterUserCommand("Ann@Example.com", "Ann Lee", "correct horse battery")
        );

        assertThat(hashed).containsExactly("correct horse battery");
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getEmail().value()).isEqualTo("ann@example.com");
        assertThat(saved.getFirst().getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(saved.getFirst().getPasswordHash().value())
            .isEqualTo("hashed:correct horse battery");

        assertThat(result.accessToken()).isEqualTo("access-for-CUSTOMER");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");

        assertThat(published).singleElement()
            .isInstanceOf(UserRegistered.class);
        assertThat(((UserRegistered) published.getFirst()).userId())
            .isEqualTo(saved.getFirst().getId());
    }

    @Test
    void neverPutsPlaintextInTheCommandsToString() {
        String rendered =
            new RegisterUserCommand("ann@example.com", "Ann Lee", "hunter2").toString();

        assertThat(rendered).doesNotContain("hunter2");
    }
}
