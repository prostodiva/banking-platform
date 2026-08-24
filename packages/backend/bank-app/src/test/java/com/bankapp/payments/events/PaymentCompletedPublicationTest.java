package com.bankapp.payments.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bankapp.TestcontainersConfiguration;
import com.bankapp.accounts.application.depositmoney.DepositMoneyCommand;
import com.bankapp.accounts.application.depositmoney.DepositMoneyHandler;
import com.bankapp.accounts.application.openaccount.OpenAccountCommand;
import com.bankapp.accounts.application.openaccount.OpenAccountHandler;
import com.bankapp.accounts.domain.AccountRepository;
import com.bankapp.accounts.domain.AccountType;
import com.bankapp.payments.application.transfermoney.TransferMoneyCommand;
import com.bankapp.payments.application.transfermoney.TransferMoneyHandler;
import com.bankapp.payments.domain.events.PaymentCompleted;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ADR-003 §5 and docs/06 promise one PaymentCompleted per <em>committed</em>
 * transfer. The listener here is a plain {@code @EventListener} on purpose: that
 * is the subscription that sees everything the publisher emits, so if events
 * escaped before commit these tests would catch it.
 */
@SpringBootTest
@Import({
    TestcontainersConfiguration.class,
    PaymentCompletedPublicationTest.RecordingListener.class
})
public class PaymentCompletedPublicationTest {

    static class RecordingListener {

        private final List<PaymentCompleted> received = new CopyOnWriteArrayList<>();

        @EventListener
        void on(PaymentCompleted event) {
            received.add(event);
        }
    }

    @Autowired
    private RecordingListener listener;

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

    private BigDecimal balanceOf(UUID accountId) {
        return accounts.findById(accountId).orElseThrow().getBalance().amount();
    }

    private static final UUID DEV_USER_ID = UUID.fromString(
        "11111111-1111-1111-1111-111111111111"
    );

    @BeforeEach
    void clearRecordedEvents() {
        listener.received.clear();
    }

    private UUID account(String initialDeposit) {
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

    private TransferMoneyCommand transfer(UUID from, UUID to, String amount) {
        return new TransferMoneyCommand(
            from,
            to,
            new BigDecimal(amount),
            "USD",
            UUID.randomUUID().toString()
        );
    }

    @Test
    void aCommittedTransferPublishesExactlyOneEvent() {
        UUID from = account("100.00");
        UUID to = account(null);

        transferMoney.handle(transfer(from, to, "25.00"));

        assertThat(listener.received)
            .singleElement()
            .satisfies(event -> {
                assertThat(event.fromAccountId()).isEqualTo(from);
                assertThat(event.toAccountId()).isEqualTo(to);
                assertThat(event.amount()).isEqualByComparingTo("25.00");
            });
    }

    /**
     * The handler runs to completion — publish included — inside an outer
     * transaction that then rolls back. This is the shape of the real failure: a
     * concurrent duplicate publishes, then loses at the unique index on flush.
     *
     * <p>Publishing on the spot fails this test. Refusing the transfer instead
     * would not: {@code moveMoney} runs before the publish, so a rejected transfer
     * never reaches it and proves nothing about when events are delivered.
     */
    @Test
    void aRollbackAfterPublishingDeliversNothing() {
        UUID from = account("100.00");
        UUID to = account(null);

        transactions.execute(status -> {
            transferMoney.handle(transfer(from, to, "25.00"));
            status.setRollbackOnly();
            return null;
        });

        assertThat(listener.received).isEmpty();
        assertThat(balanceOf(from)).isEqualByComparingTo("100.00");
    }

    /** Money is refused before the publish call, so nothing is announced either. */
    @Test
    void aRefusedTransferPublishesNothing() {
        UUID from = account("10.00");
        UUID to = account(null);

        assertThatThrownBy(() -> transferMoney.handle(transfer(from, to, "25.00")))
            .isInstanceOf(IllegalStateException.class);

        assertThat(listener.received).isEmpty();
    }

    @Test
    void aReplayedTransferPublishesNothingSecondTime() {
        UUID from = account("100.00");
        UUID to = account(null);
        TransferMoneyCommand command = transfer(from, to, "25.00");

        transferMoney.handle(command);
        transferMoney.handle(command);
        transferMoney.handle(command);

        assertThat(listener.received).hasSize(1);
    }
}
