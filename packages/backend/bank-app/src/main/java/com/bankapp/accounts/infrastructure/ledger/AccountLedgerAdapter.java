package com.bankapp.accounts.infrastructure.ledger;

import com.bankapp.accounts.application.port.AccountLedger;
import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountRepository;
import com.bankapp.accounts.domain.exceptions.AccountNotFoundException;
import com.bankapp.shared.domain.Money;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the published ledger port by delegating to the accounts aggregate:
 * the balance rules live in {@link Account} and are not restated here (ADR-003 §1).
 *
 * <p>Publishes nothing. The caller owns the business fact (ADR-003 §5), so
 * reusing the deposit/withdraw handlers — and firing their events — would tell
 * one customer three things about one transfer.
 */
@Service
class AccountLedgerAdapter implements AccountLedger {

    private final AccountRepository accounts;

    AccountLedgerAdapter(AccountRepository accounts) {
        this.accounts = accounts;
    }

    /**
     * MANDATORY, not REQUIRED: this joins the caller's transaction and never
     * starts one (ADR-003 §3). Moving money outside a transaction is never
     * correct, so a missing boundary should fail loudly rather than commit each
     * leg on its own.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void moveMoney(UUID fromAccountId, UUID toAccountId, Money amount) {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException(
                "cannot move money to the same account"
            );
        }

        // Sorted id order, so concurrent A→B and B→A transfers touch the two rows
        // in the same order (ADR-003 §3).
        //
        // Note what actually takes the locks: findById does not — it is a plain
        // SELECT. The row locks come from the UPDATEs Hibernate emits at flush, so
        // the ordering that matters is theirs, and that is enforced by
        // `hibernate.order_updates=true` in application.properties (sorts updates
        // by primary key). Loading in the same order keeps this code honest and
        // readable, but the property is the guarantee — don't remove it.
        boolean fromIsLower = fromAccountId.compareTo(toAccountId) < 0;
        Account lower = load(fromIsLower ? fromAccountId : toAccountId);
        Account higher = load(fromIsLower ? toAccountId : fromAccountId);

        Account source = fromIsLower ? lower : higher;
        Account target = fromIsLower ? higher : lower;

        // Withdraw first: insufficient funds and a frozen source both refuse
        // here, before anything has been credited.
        source.withdraw(amount);
        target.deposit(amount);

        accounts.save(source);
        accounts.save(target);
    }

    private Account load(UUID accountId) {
        return accounts
            .findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}
