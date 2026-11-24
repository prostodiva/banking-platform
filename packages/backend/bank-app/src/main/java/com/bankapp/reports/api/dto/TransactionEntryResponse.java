package com.bankapp.reports.api.dto;

import com.bankapp.reports.application.shared.TransactionEntryView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionEntryResponse(
    UUID id,
    String type,
    BigDecimal amount,
    String currencyCode,
    UUID counterpartyAccountId,
    Instant occurredAt
) {
    public static TransactionEntryResponse from(TransactionEntryView view) {
        return new TransactionEntryResponse(
            view.id(),
            view.type().name(),
            view.amount(),
            view.currencyCode(),
            view.counterpartyAccountId(),
            view.occurredAt()
        );
    }
}
