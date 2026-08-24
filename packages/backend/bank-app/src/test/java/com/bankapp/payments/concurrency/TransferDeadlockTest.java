package com.bankapp.payments.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankapp.TestcontainersConfiguration;
import com.bankapp.accounts.application.depositmoney.DepositMoneyCommand;
import com.bankapp.accounts.application.depositmoney.DepositMoneyHandler;
import com.bankapp.accounts.application.openaccount.OpenAccountCommand;
import com.bankapp.accounts.application.openaccount.OpenAccountHandler;
import com.bankapp.accounts.domain.AccountRepository;
import com.bankapp.accounts.domain.AccountType;
import com.bankapp.payments.application.transfermoney.TransferMoneyCommand;
import com.bankapp.payments.application.transfermoney.TransferMoneyHandler;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

/**
 * The A→B / B→A deadlock ADR-003 §3 guards against. Two transfers in opposite
 * directions touch the same two `accounts` rows; if they locked them in opposite
 * orders, each would hold what the other needs.
 *
 * <p>What prevents it is `hibernate.order_updates=true` — Hibernate sorts
 * flush-time UPDATEs by primary key, so every transaction takes the two rows in
 * the same order. Deleting that property makes this test fail intermittently
 * under load, which is the failure mode it exists to document.
 *
 * <p>Timing decides whether the transactions actually overlap, so this asserts an
 * invariant rather than a specific outcome: money is conserved, nothing hangs, and
 * any failure is a retryable conflict — never a deadlock reaching the caller.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class TransferDeadlockTest {

    private static final int ROUNDS = 8;

    @Autowired
    private OpenAccountHandler openAccount;

    @Autowired
    private DepositMoneyHandler depositMoney;

    @Autowired
    private TransferMoneyHandler transferMoney;

    @Autowired
    private AccountRepository accounts;

    private static final UUID DEV_USER_ID = UUID.fromString(
        "11111111-1111-1111-1111-111111111111"
    );

    private UUID fundedAccount(String amount) {
        UUID id = openAccount
            .handle(new OpenAccountCommand(DEV_USER_ID, AccountType.CHECKING, "USD"))
            .id();
        depositMoney.handle(
            new DepositMoneyCommand(id, new BigDecimal(amount), "USD")
        );
        return id;
    }

    private BigDecimal balanceOf(UUID accountId) {
        return accounts.findById(accountId).orElseThrow().getBalance().amount();
    }

    @Test
    void oppositeDirectionTransfersDoNotDeadlock() throws Exception {
        UUID a = fundedAccount("100.00");
        UUID b = fundedAccount("100.00");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Throwable> failures = new ArrayList<>();
        try {
            for (int round = 0; round < ROUNDS; round++) {
                CyclicBarrier startTogether = new CyclicBarrier(2);

                Future<Optional<Throwable>> aToB = pool.submit(
                    attempt(startTogether, a, b)
                );
                Future<Optional<Throwable>> bToA = pool.submit(
                    attempt(startTogether, b, a)
                );

                aToB.get().ifPresent(failures::add);
                bToA.get().ifPresent(failures::add);
            }
        } finally {
            pool.shutdownNow();
        }

        // Nothing hung, and the pair still holds the money it started with.
        assertThat(balanceOf(a).add(balanceOf(b))).isEqualByComparingTo("200.00");

        // A deadlock victim would arrive as DeadlockLoserDataAccessException.
        // Losing on @Version is expected and fine; deadlocking is not.
        assertThat(failures).allSatisfy(failure ->
            assertThat(failure).isInstanceOf(OptimisticLockingFailureException.class)
        );
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
}
