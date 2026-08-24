package com.bankapp.payments.api.dto;

import com.bankapp.payments.application.gettransfer.TransferView;
import com.bankapp.payments.application.transfermoney.TransferMoneyResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Returned by both endpoints: 201 on the transfer that just happened, 200 on
 * {@code GET /api/payments/transfers/{id}} (FR-PAY-02), and 201 again on a
 * replay — the whole point of idempotency is that a retry cannot tell the
 * difference (ADR-003 §6).
 */
public record TransferResponse(
    UUID id,
    UUID fromAccountId,
    UUID toAccountId,
    BigDecimal amount,
    String currencyCode,
    Instant createdAt
) {
    public static TransferResponse from(TransferMoneyResult result) {
        return new TransferResponse(
            result.id(),
            result.fromAccountId(),
            result.toAccountId(),
            result.amount(),
            result.currencyCode(),
            result.createdAt()
        );
    }

    public static TransferResponse from(TransferView view) {
        return new TransferResponse(
            view.id(),
            view.fromAccountId(),
            view.toAccountId(),
            view.amount(),
            view.currencyCode(),
            view.createdAt()
        );
    }
}
