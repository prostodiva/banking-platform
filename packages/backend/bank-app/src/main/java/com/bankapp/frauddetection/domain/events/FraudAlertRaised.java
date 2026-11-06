package com.bankapp.frauddetection.domain.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudAlertRaised(
    UUID alertId,
    UUID transferId,
    UUID fromAccountId,
    BigDecimal amount,
    String currencyCode,
    Instant occurredAt
) {}
