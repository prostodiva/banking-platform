package com.bankapp.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UserTest {

    private static final Email EMAIL = new Email("ann@example.com");
    private static final PasswordHash HASH = new PasswordHash("$2b$12$notarealhash");

    @Test
    void registersAsCustomer() {
        User user = User.register(EMAIL, "Ann Lee", HASH);

        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void thereIsNoWayToRegisterAnAdmin() {
        // Not an assertion you can write — it is enforced by the absence of a
        // parameter. If this test ever becomes writable, decision 5 has been broken.
        assertThat(User.class.getDeclaredMethods())
            .filteredOn(m -> m.getName().equals("register"))
            .allSatisfy(m -> assertThat(m.getParameterTypes()).doesNotContain(Role.class));
    }

    @Test
    void rejectsBlankFullName() {
        assertThatThrownBy(() -> User.register(EMAIL, "   ", HASH))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fullName");
    }

    @Test
    void doesNotLeakCredentialsInToString() {
        User user = User.register(EMAIL, "Ann Lee", HASH);

        assertThat(user.toString()).doesNotContain("notarealhash");
    }
}
