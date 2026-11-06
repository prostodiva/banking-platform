package com.bankapp.frauddetection.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bankapp.shared.domain.Money;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FraudAlertTest {

    private static final UUID TRANSFER = UUID.randomUUID();
    private static final UUID FROM_ACCOUNT = UUID.randomUUID();

    private static Money amount(String value) {
        return new Money(new BigDecimal(value), "USD");
    }

    @Test
    void raiseCreatesAlertInRaisedStatus() {
        FraudAlert alert = FraudAlert.raise(TRANSFER, FROM_ACCOUNT, amount("15000"));

        assertThat(alert.getId()).isNotNull();
        assertThat(alert.getTransferId()).isEqualTo(TRANSFER);
        assertThat(alert.getFromAccountId()).isEqualTo(FROM_ACCOUNT);
        assertThat(alert.getAmount().amount()).isEqualByComparingTo("15000");
        assertThat(alert.getReason()).isEqualTo(FraudAlertReason.LARGE_PAYMENT);
        assertThat(alert.getStatus()).isEqualTo(FraudAlertStatus.RAISED);
        assertThat(alert.getCreatedAt()).isNotNull();
    }

    @Test
    void raiseRequiresTransferId() {
        assertThatThrownBy(() -> FraudAlert.raise(null, FROM_ACCOUNT, amount("15000")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void raiseRequiresFromAccountId() {
        assertThatThrownBy(() -> FraudAlert.raise(TRANSFER, null, amount("15000")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void raiseRequiresAmount() {
        assertThatThrownBy(() -> FraudAlert.raise(TRANSFER, FROM_ACCOUNT, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
