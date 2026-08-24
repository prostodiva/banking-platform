package com.bankapp.payments.domain.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCompleted(UUID transferId, UUID fromAccountId, UUID toAccountId, BigDecimal amount, String currencyCode, String idempotencyKey, Instant occurredAt) {
}
