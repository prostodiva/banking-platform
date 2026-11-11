package com.bankapp.notifications.infrastructure.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudAlertRaisedMessage(
        UUID alertId,
        UUID transferId,
        UUID fromAccountId,
        BigDecimal amount,
        String currencyCode,
        Instant occurredAt
) {
}
