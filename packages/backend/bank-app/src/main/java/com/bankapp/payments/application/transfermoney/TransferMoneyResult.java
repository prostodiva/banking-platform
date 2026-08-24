package com.bankapp.payments.application.transfermoney;

import com.bankapp.payments.domain.Transfer;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferMoneyResult(
    UUID id,
    UUID fromAccountId,
    UUID toAccountId,
    BigDecimal amount,
    String currencyCode,
    Instant createdAt
) {
    public static TransferMoneyResult from(Transfer transfer) {
        return new TransferMoneyResult(
            transfer.getId(),
            transfer.getFromAccountId(),
            transfer.getToAccountId(),
            transfer.getAmount().amount(),
            transfer.getAmount().currencyCode(),
            transfer.getCreatedAt()
        );
    }
}
