package com.bankapp.reports.application.projectpayment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProjectPaymentCommand(
    UUID transferId,
    UUID fromAccountId,
    UUID toAccountId,
    BigDecimal amount,
    String currencyCode,
    Instant occurredAt
) {}
