package com.bankapp.reports.infrastructure.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record PaymentCompletedMessage(
    UUID transferId,
    UUID fromAccountId,
    UUID toAccountId,
    BigDecimal amount,
    String currencyCode,
    String idempotencyKey,
    Instant occurredAt
) {}
