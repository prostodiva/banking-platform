package com.bankapp.payments.application.transfermoney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bankapp.accounts.application.port.AccountLedger;
import com.bankapp.payments.domain.Transfer;
import com.bankapp.payments.domain.TransferRepository;
import com.bankapp.payments.domain.events.PaymentCompleted;
import com.bankapp.payments.domain.exceptions.IdempotencyKeyConflictException;
import com.bankapp.shared.domain.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransferMoneyHandlerTest {

    private static final UUID FROM = UUID.randomUUID();
    private static final UUID TO = UUID.randomUUID();
    private static final String KEY = "idem-key-1";

    private final Map<UUID, Transfer> saved = new HashMap<>();
    private final List<Object> published = new ArrayList<>();
    private final List<Movement> movements = new ArrayList<>();

    /** What the ledger was asked to do — the port is the only view of accounts. */
    private record Movement(UUID from, UUID to, Money amount) {}

    private RuntimeException ledgerFailure;

    private final TransferRepository transfers = new TransferRepository() {
        @Override
        public Transfer save(Transfer transfer) {
            saved.put(transfer.getId(), transfer);
            return transfer;
        }

        @Override
        public Optional<Transfer> findById(UUID id) {
            return Optional.ofNullable(saved.get(id));
        }

        @Override
        public Optional<Transfer> findByIdempotencyKey(String idempotencyKey) {
            return saved
                .values()
                .stream()
                .filter(t -> t.getIdempotencyKey().equals(idempotencyKey))
                .findFirst();
        }
    };

    private final AccountLedger ledger = (from, to, amount) -> {
        if (ledgerFailure != null) {
            throw ledgerFailure;
        }
        movements.add(new Movement(from, to, amount));
    };

    private final TransferMoneyHandler handler = new TransferMoneyHandler(
        transfers,
        ledger,
        published::add
    );

    private static TransferMoneyCommand command(String amount, String key) {
        return new TransferMoneyCommand(
            FROM,
            TO,
            new BigDecimal(amount),
            "USD",
            key
        );
    }

    @Test
    void recordsTheTransferAndMovesTheMoney() {
        TransferMoneyResult result = handler.handle(command("25.00", KEY));

        assertThat(saved).containsKey(result.id());
        assertThat(result.fromAccountId()).isEqualTo(FROM);
        assertThat(result.toAccountId()).isEqualTo(TO);
        assertThat(result.amount()).isEqualByComparingTo("25.00");
        assertThat(movements).containsExactly(
            new Movement(FROM, TO, new Money(new BigDecimal("25.00"), "USD"))
        );
    }

    @Test
    void publishesPaymentCompletedCarryingTheTransferId() {
        TransferMoneyResult result = handler.handle(command("25.00", KEY));

        assertThat(published)
            .singleElement()
            .isInstanceOfSatisfying(PaymentCompleted.class, event -> {
                assertThat(event.transferId()).isEqualTo(result.id());
                assertThat(event.fromAccountId()).isEqualTo(FROM);
                assertThat(event.toAccountId()).isEqualTo(TO);
                assertThat(event.idempotencyKey()).isEqualTo(KEY);
            });
    }

    @Test
    void replayReturnsTheOriginalWithoutMovingMoneyAgain() {
        TransferMoneyResult first = handler.handle(command("25.00", KEY));
        TransferMoneyResult replay = handler.handle(command("25.00", KEY));

        assertThat(replay).isEqualTo(first);
        assertThat(saved).hasSize(1);
        assertThat(movements).hasSize(1);
    }

    /** One retry must not become five payments downstream (ADR-003 §5). */
    @Test
    void replayPublishesNothing() {
        handler.handle(command("25.00", KEY));
        handler.handle(command("25.00", KEY));
        handler.handle(command("25.00", KEY));

        assertThat(published).hasSize(1);
    }

    @Test
    void sameKeyWithADifferentAmountIsRefused() {
        handler.handle(command("25.00", KEY));

        assertThatThrownBy(() -> handler.handle(command("30.00", KEY)))
            .isInstanceOf(IdempotencyKeyConflictException.class);

        assertThat(saved).hasSize(1);
        assertThat(movements).hasSize(1);
        assertThat(published).hasSize(1);
    }

    @Test
    void differentKeysAreDifferentTransfers() {
        handler.handle(command("25.00", KEY));
        handler.handle(command("25.00", "idem-key-2"));

        assertThat(saved).hasSize(2);
        assertThat(movements).hasSize(2);
        assertThat(published).hasSize(2);
    }

    /** A refused movement leaves no transfer row and no event (ADR-003 §4). */
    @Test
    void aFailedMovementRecordsNothing() {
        ledgerFailure = new IllegalStateException("insufficient funds");

        assertThatThrownBy(() -> handler.handle(command("25.00", KEY)))
            .isInstanceOf(IllegalStateException.class);

        assertThat(saved).isEmpty();
        assertThat(published).isEmpty();
    }

    @Test
    void aRejectedRequestNeverReachesTheLedger() {
        TransferMoneyCommand sameAccount = new TransferMoneyCommand(
            FROM,
            FROM,
            new BigDecimal("25.00"),
            "USD",
            KEY
        );

        assertThatThrownBy(() -> handler.handle(sameAccount))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(movements).isEmpty();
        assertThat(saved).isEmpty();
    }
}
