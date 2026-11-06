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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ADR-003 §5 and docs/06 promise one PaymentCompleted per <em>committed</em>
 * transfer. The consumer here subscribes to the payment-completed topic with
 * a throwaway group: that is the subscription that sees everything the
 * publisher emits, so if events escaped before commit these tests would
 * catch it.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class PaymentCompletedPublicationTest {

    private static final String TOPIC = "payment-completed";

    @Autowired
    private ConsumerFactory<Object, Object> consumerFactory;

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

    private Consumer<Object, Object> consumer;

    private static final UUID DEV_USER_ID = UUID.fromString(
        "11111111-1111-1111-1111-111111111111"
    );

    @BeforeEach
    void startConsuming() {
        consumer = consumerFactory.createConsumer(
            "publication-test-" + UUID.randomUUID(),
            null
        );
        consumer.subscribe(List.of(TOPIC));
    }

    @AfterEach
    void stopConsuming() {
        consumer.close();
    }

    /** Polls until at least one matching event arrives or the timeout passes. */
    private List<PaymentCompleted> awaitMatching(
        Predicate<PaymentCompleted> match,
        Duration timeout
    ) {
        List<PaymentCompleted> found = new ArrayList<>();
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (found.isEmpty() && System.currentTimeMillis() < deadline) {
            pollOnce(match, found, Duration.ofMillis(200));
        }
        return found;
    }

    /** Polls for the full window, collecting every matching event. */
    private List<PaymentCompleted> collectDuring(
        Predicate<PaymentCompleted> match,
        Duration window
    ) {
        List<PaymentCompleted> found = new ArrayList<>();
        long deadline = System.currentTimeMillis() + window.toMillis();
        while (System.currentTimeMillis() < deadline) {
            pollOnce(match, found, Duration.ofMillis(200));
        }
        return found;
    }

    private void pollOnce(
        Predicate<PaymentCompleted> match,
        List<PaymentCompleted> found,
        Duration timeout
    ) {
        ConsumerRecords<Object, Object> records = consumer.poll(timeout);
        records.forEach(record -> {
            PaymentCompleted event = (PaymentCompleted) record.value();
            if (match.test(event)) {
                found.add(event);
            }
        });
    }

    private BigDecimal balanceOf(UUID accountId) {
        return accounts.findById(accountId).orElseThrow().getBalance().amount();
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

        var result = transferMoney.handle(transfer(from, to, "25.00"));

        List<PaymentCompleted> events = awaitMatching(
            event -> event.transferId().equals(result.id()),
            Duration.ofSeconds(10)
        );
        assertThat(events)
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

        List<PaymentCompleted> events = collectDuring(
            event -> event.fromAccountId().equals(from),
            Duration.ofSeconds(3)
        );
        assertThat(events).isEmpty();
        assertThat(balanceOf(from)).isEqualByComparingTo("100.00");
    }

    /** Money is refused before the publish call, so nothing is announced either. */
    @Test
    void aRefusedTransferPublishesNothing() {
        UUID from = account("10.00");
        UUID to = account(null);

        assertThatThrownBy(() -> transferMoney.handle(transfer(from, to, "25.00")))
            .isInstanceOf(IllegalStateException.class);

        List<PaymentCompleted> events = collectDuring(
            event -> event.fromAccountId().equals(from),
            Duration.ofSeconds(3)
        );
        assertThat(events).isEmpty();
    }

    @Test
    void aReplayedTransferPublishesNothingSecondTime() {
        UUID from = account("100.00");
        UUID to = account(null);
        TransferMoneyCommand command = transfer(from, to, "25.00");

        var result = transferMoney.handle(command);
        transferMoney.handle(command);
        transferMoney.handle(command);

        Predicate<PaymentCompleted> mine =
            event -> event.transferId().equals(result.id());
        List<PaymentCompleted> events = awaitMatching(mine, Duration.ofSeconds(10));
        events.addAll(collectDuring(mine, Duration.ofSeconds(2)));
        assertThat(events).hasSize(1);
    }
}
