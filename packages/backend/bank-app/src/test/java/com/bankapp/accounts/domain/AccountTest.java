package com.bankapp.accounts.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountTest {

    private static final UUID OWNER = UUID.randomUUID();

    @Test
    void openCreatesActiveAccountWithZeroBalance() {
        Account account = Account.open(
            OWNER,
            AccountType.CHECKING,
            "USD",
            "1233445555"
        );

        assertThat(account.getId()).isNotNull();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getBalance().amount()).isEqualByComparingTo(
            BigDecimal.ZERO
        );
        assertThat(account.getBalance().currencyCode()).isEqualTo("USD");
        assertThat(account.getOwnerId()).isEqualTo(OWNER);
    }

    @Test
    void openRequiresAnOwner() {
        assertThatThrownBy(() ->
            Account.open(null, AccountType.CHECKING, "USD", "1234567890")
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void openRejectsInvalidCurrency() {
        assertThatThrownBy(() ->
            Account.open(OWNER, AccountType.SAVINGS, "DOLLARS", "1234567890")
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
