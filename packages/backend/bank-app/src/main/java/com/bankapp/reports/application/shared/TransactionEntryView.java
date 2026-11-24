package com.bankapp.reports.application.shared;

import com.bankapp.reports.domain.TransactionEntry;
import com.bankapp.reports.domain.TransactionEntryType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionEntryView(
    UUID id,
    TransactionEntryType type,
    BigDecimal amount,
    String currencyCode,
    UUID counterpartyAccountId,
    Instant occurredAt
) {
    public static TransactionEntryView from(TransactionEntry entry) {
        return new TransactionEntryView(
            entry.getId(),
            entry.getType(),
            entry.getAmount().amount(),
            entry.getAmount().currencyCode(),
            entry.getCounterpartyAccountId(),
            entry.getOccurredAt()
        );
    }
}
