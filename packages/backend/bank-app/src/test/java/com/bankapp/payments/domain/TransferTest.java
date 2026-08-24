package com.bankapp.payments.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bankapp.shared.domain.Money;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransferTest {

    private static final UUID FROM = UUID.randomUUID();
    private static final UUID TO = UUID.randomUUID();
    private static final String KEY = "idem-key-1";

    private static Money usd(String amount) {
        return new Money(new BigDecimal(amount), "USD");
    }

    @Test
    void recordCapturesAMovementThatAlreadyHappened() {
        Transfer transfer = Transfer.record(FROM, TO, usd("25.00"), KEY);

        assertThat(transfer.getId()).isNotNull();
        assertThat(transfer.getFromAccountId()).isEqualTo(FROM);
        assertThat(transfer.getToAccountId()).isEqualTo(TO);
        assertThat(transfer.getAmount()).isEqualTo(usd("25.00"));
        assertThat(transfer.getIdempotencyKey()).isEqualTo(KEY);
        assertThat(transfer.getCreatedAt()).isNotNull();
    }

    @Test
    void recordRejectsATransferToTheSameAccount() {
        assertThatThrownBy(() -> Transfer.record(FROM, FROM, usd("25.00"), KEY))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recordRequiresBothAccounts() {
        assertThatThrownBy(() -> Transfer.record(null, TO, usd("25.00"), KEY))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Transfer.record(FROM, null, usd("25.00"), KEY))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recordRejectsANonPositiveAmount() {
        assertThatThrownBy(() -> Transfer.record(FROM, TO, usd("0.00"), KEY))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Transfer.record(FROM, TO, usd("-5.00"), KEY))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recordRequiresAnIdempotencyKey() {
        assertThatThrownBy(() -> Transfer.record(FROM, TO, usd("25.00"), null))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Transfer.record(FROM, TO, usd("25.00"), "  "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    /** varchar(64): caught here as a 400 rather than surfacing as a DB error. */
    @Test
    void recordRejectsAnOverLongIdempotencyKey() {
        assertThatThrownBy(() ->
            Transfer.record(FROM, TO, usd("25.00"), "k".repeat(65))
        ).isInstanceOf(IllegalArgumentException.class);

        assertThat(Transfer.record(FROM, TO, usd("25.00"), "k".repeat(64)))
            .isNotNull();
    }

    @Test
    void recordsTheSameMovementIsTrueForAGenuineReplay() {
        Transfer transfer = Transfer.record(FROM, TO, usd("25.00"), KEY);

        assertThat(transfer.records(FROM, TO, usd("25.00"))).isTrue();
    }

    /** Money normalizes scale, so "25.00" and "25.0000" are the same movement. */
    @Test
    void recordsIgnoresScaleDifferencesInTheAmount() {
        Transfer transfer = Transfer.record(FROM, TO, usd("25.00"), KEY);

        assertThat(transfer.records(FROM, TO, usd("25.0000"))).isTrue();
    }

    @Test
    void recordsIsFalseWhenAnyFieldDiffers() {
        Transfer transfer = Transfer.record(FROM, TO, usd("25.00"), KEY);

        assertThat(transfer.records(FROM, TO, usd("30.00"))).isFalse();
        assertThat(transfer.records(TO, FROM, usd("25.00"))).isFalse();
        assertThat(transfer.records(FROM, UUID.randomUUID(), usd("25.00")))
            .isFalse();
        assertThat(
            transfer.records(FROM, TO, new Money(new BigDecimal("25.00"), "EUR"))
        ).isFalse();
    }
}
