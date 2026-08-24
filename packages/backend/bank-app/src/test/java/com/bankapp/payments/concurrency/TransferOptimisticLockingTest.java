package com.bankapp.payments.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bankapp.TestcontainersConfiguration;
import com.bankapp.accounts.application.depositmoney.DepositMoneyCommand;
import com.bankapp.accounts.application.depositmoney.DepositMoneyHandler;
import com.bankapp.accounts.application.openaccount.OpenAccountCommand;
import com.bankapp.accounts.application.openaccount.OpenAccountHandler;
import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountRepository;
import com.bankapp.accounts.domain.AccountType;
import com.bankapp.payments.application.transfermoney.TransferMoneyCommand;
import com.bankapp.payments.application.transfermoney.TransferMoneyHandler;
import com.bankapp.shared.domain.Money;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ADR-003 §3 leans on {@code @Version} (ADR-002) to catch lost updates, and maps
 * the result to 409. No HTTP here — driving the handler directly is the only way
 * to control transaction boundaries precisely enough to provoke the collision.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class TransferOptimisticLockingTest {

    @Autowired
    private OpenAccountHandler openAccount;

    @Autowired
    private DepositMoneyHandler depositMoney;

    @Autowired
    private TransferMoneyHandler transferMoney;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private JdbcTemplate jdbc;

    private static final UUID DEV_USER_ID = UUID.fromString(
        "11111111-1111-1111-1111-111111111111"
    );

    private UUID openAccount(String initialDeposit) {
        UUID id = openAccount
            .handle(new OpenAccountCommand(DEV_USER_ID, AccountType.CHECKING, "USD"))
            .id();
        if (initialDeposit != null) {
            depositMoney.handle(
                new DepositMoneyCommand(id, new BigDecimal(initialDeposit), "USD")
            );
        }
        return id;
    }

    private BigDecimal balanceOf(UUID accountId) {
        return accounts
            .findById(accountId)
            .orElseThrow()
            .getBalance()
            .amount();
    }

    /**
     * Deterministic: the entity is loaded at version N, the row is moved to N+1
     * underneath it, and the write-back then matches no row. This is the exact
     * exception GlobalExceptionHandler turns into a 409.
     */
    @Test
    void aWriteOverAStaleVersionFailsWithOptimisticLocking() {
        UUID accountId = openAccount("100.00");

        assertThatThrownBy(() ->
            transactions.executeWithoutResult(status -> {
                Account account = accounts.findById(accountId).orElseThrow();

                jdbc.update(
                    "update accounts set version = version + 1 where id = ?",
                    accountId
                );

                account.deposit(new Money(new BigDecimal("1.00"), "USD"));
                accounts.save(account);
            })
        ).isInstanceOf(OptimisticLockingFailureException.class);
    }

    /**
     * Two real transfers out of one account, started together. Either both
     * serialize cleanly or one loses on {@code @Version} — never a lost update,
     * which is what NFR-02 actually promises.
     */
    @Test
    void concurrentTransfersOutOfOneAccountNeverLoseMoney() throws Exception {
        UUID source = openAccount("100.00");
        UUID first = openAccount(null);
        UUID second = openAccount(null);

        CyclicBarrier startTogether = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            Future<Optional<Throwable>> a = pool.submit(
                attempt(startTogether, source, first)
            );
            Future<Optional<Throwable>> b = pool.submit(
                attempt(startTogether, source, second)
            );

            Optional<Throwable> failureA = a.get();
            Optional<Throwable> failureB = b.get();

            // Whatever happened, no money was created or destroyed.
            BigDecimal total = balanceOf(source)
                .add(balanceOf(first))
                .add(balanceOf(second));
            assertThat(total).isEqualByComparingTo("100.00");

            // At most one may fail, and only for the documented reason.
            assertThat(failureA.isPresent() && failureB.isPresent()).isFalse();
            failureA.ifPresent(this::assertIsAConflict);
            failureB.ifPresent(this::assertIsAConflict);

            // The debits that did succeed are the only ones reflected.
            BigDecimal succeeded = BigDecimal.valueOf(
                2 - (failureA.isPresent() ? 1 : 0) - (failureB.isPresent() ? 1 : 0)
            ).multiply(new BigDecimal("10.00"));
            assertThat(balanceOf(source)).isEqualByComparingTo(
                new BigDecimal("100.00").subtract(succeeded)
            );
        } finally {
            pool.shutdownNow();
        }
    }

    private Callable<Optional<Throwable>> attempt(
        CyclicBarrier startTogether,
        UUID from,
        UUID to
    ) {
        return () -> {
            startTogether.await();
            try {
                transferMoney.handle(
                    new TransferMoneyCommand(
                        from,
                        to,
                        new BigDecimal("10.00"),
                        "USD",
                        UUID.randomUUID().toString()
                    )
                );
                return Optional.empty();
            } catch (Throwable failure) {
                return Optional.of(failure);
            }
        };
    }

    private void assertIsAConflict(Throwable failure) {
        assertThat(failure).isInstanceOf(OptimisticLockingFailureException.class);
    }
}
