package com.bankapp.accounts.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bankapp.shared.domain.Money;
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
    void freezeMakesActiveAccountFrozen() {
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

    @Test
    void closeRejectsAccountWithNonZeroBalance() {
        Account account = activeAccount();
        account.deposit(new Money(new BigDecimal("100.00"), "USD"));

        assertThatThrownBy(account::close).isInstanceOf(
            IllegalStateException.class
        );
    }

    @Test
    void depositRejectsFrozenAccount() {
        Account account = activeAccount();
        account.freeze();

        assertThatThrownBy(() ->
            account.deposit(new Money(new BigDecimal("50.00"), "USD"))
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void depositRejectsClosedAccount() {
        Account account = activeAccount();
        account.close();

        assertThatThrownBy(() ->
            account.deposit(new Money(new BigDecimal("50.00"), "USD"))
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void withdrawRejectsClosedAccount() {
        Account account = activeAccount();
        account.close();

        assertThatThrownBy(() ->
            account.withdraw(new Money(new BigDecimal("50.00"), "USD"))
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void withdrawRejectsFrozenAccount() {
        Account account = activeAccount();
        account.freeze();

        assertThatThrownBy(() ->
            account.withdraw(new Money(new BigDecimal("50.00"), "USD"))
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void depositRejectsNonPositiveAmount() {
        Account account = activeAccount();

        assertThatThrownBy(() ->
            account.deposit(new Money(new BigDecimal("-50.00"), "USD"))
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
            account.deposit(Money.zero("USD"))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withdrawRejectsNonPositiveAmount() {
        Account account = activeAccount();

        assertThatThrownBy(() ->
            account.withdraw(new Money(new BigDecimal("-50.00"), "USD"))
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
            account.withdraw(Money.zero("USD"))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void depositRejectsMismatchedCurrencyAmount() {
        Account account = activeAccount();

        assertThatThrownBy(() ->
            account.deposit(new Money(new BigDecimal("50.00"), "RUB"))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withdrawRejectsMismatchedCurrencyAmount() {
        Account account = activeAccount();

        assertThatThrownBy(() ->
            account.withdraw(new Money(new BigDecimal("50.00"), "RUB"))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withdrawRejectsAmountAboveBalance() {
        Account account = activeAccount();
        account.deposit(new Money(new BigDecimal("50.00"), "USD"));

        assertThatThrownBy(() ->
            account.withdraw(new Money(new BigDecimal("100.00"), "USD"))
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void depositIncreasesBalance() {
        Account account = activeAccount();

        account.deposit(new Money(new BigDecimal("50.00"), "USD"));
        account.deposit(new Money(new BigDecimal("25.50"), "USD"));

        assertThat(account.getBalance().amount()).isEqualByComparingTo(
            new BigDecimal("75.50")
        );
    }

    @Test
    void withdrawDecreasesBalance() {
        Account account = activeAccount();
        account.deposit(new Money(new BigDecimal("100.00"), "USD"));

        account.withdraw(new Money(new BigDecimal("30.00"), "USD"));

        assertThat(account.getBalance().amount()).isEqualByComparingTo(
            new BigDecimal("70.00")
        );
    }

    @Test
    void withdrawAllowsExactBalance() {
        Account account = activeAccount();
        account.deposit(new Money(new BigDecimal("100.00"), "USD"));

        account.withdraw(new Money(new BigDecimal("100.00"), "USD"));

        assertThat(account.getBalance().isZero()).isTrue();
    }
}
