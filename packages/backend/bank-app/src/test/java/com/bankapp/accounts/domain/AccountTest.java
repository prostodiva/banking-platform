package com.bankapp.accounts.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountTest {

    private static final UUID OWNER = UUID.randomUUID();

    private Account activeAccount() {
        return Account.open(
            OWNER,
            AccountType.CHECKING,
            "USD",
            new AccountNumber("1234567890")
        );
    }

    @Test
    void openCreatesActiveAccountWithZeroBalance() {
        Account account = Account.open(
            OWNER,
            AccountType.CHECKING,
            "USD",
            new AccountNumber("1233445555")
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
            Account.open(
                null,
                AccountType.CHECKING,
                "USD",
                new AccountNumber("1234567890")
            )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void openRejectsInvalidCurrency() {
        assertThatThrownBy(() ->
            Account.open(
                OWNER,
                AccountType.SAVINGS,
                "DOLLARS",
                new AccountNumber("1234567890")
            )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void freezeMankesActiveAccountFrozen() {
        Account account = activeAccount();

        account.freeze();

        assertThat(account.getStatus()).isEqualTo(AccountStatus.FROZEN);
    }

    @Test
    void freezeRejectsAnAlreadyFrozenAccount() {
        Account account = activeAccount();

        account.freeze();

        assertThatThrownBy(account::freeze).isInstanceOf(
            IllegalStateException.class
        );
    }

    @Test
    void unfreezeMakesFrozenAccountActive() {
        Account account = activeAccount();

        account.freeze();
        account.unfreeze();

        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void unfreezeRejectsANonFrozenAccount() {
        Account account = activeAccount();

        assertThatThrownBy(account::unfreeze).isInstanceOf(
            IllegalStateException.class
        );
    }

    @Test
    void closeMakesActiveAccountClosed() {
        Account account = activeAccount();

        account.close();

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void closeMakesFrozenAccountClosed() {
        Account account = activeAccount();
        account.freeze();

        account.close();

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void closedAccountCannotBeFrozen() {
        Account account = activeAccount();
        account.close();

        assertThatThrownBy(account::freeze).isInstanceOf(
            IllegalStateException.class
        );
    }

    @Test
    void closedAccountCannotBeUnfrozen() {
        Account account = activeAccount();
        account.close();

        assertThatThrownBy(account::unfreeze).isInstanceOf(
            IllegalStateException.class
        );
    }

// ones deposit exists - add closeRejectsAccountWithNonZeroBalance

}
