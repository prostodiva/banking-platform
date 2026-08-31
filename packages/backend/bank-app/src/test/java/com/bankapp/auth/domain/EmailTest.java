package com.bankapp.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EmailTest {

    @Test
    void lowercasesAndTrims() {
        assertThat(new Email("  Ann@Example.COM ").value()).isEqualTo("ann@example.com");
    }

    @Test
    void twoSpellingsOfTheSameAddressAreEqual() {
        assertThat(new Email("Ann@Example.com")).isEqualTo(new Email("ann@example.com"));
    }

    @Test
    void rejectsBlankAndMalformed() {
        assertThatThrownBy(() -> new Email("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Email("ann@")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Email("ann at example.com"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
