package com.bankapp.payments.application.gettransfer;

import com.bankapp.payments.domain.Transfer;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferView(
    UUID id,
    UUID fromAccountId,
    UUID toAccountId,
    BigDecimal amount,
    String currencyCode,
    Instant createdAt
) {
    public static TransferView from(Transfer transfer) {
        return new TransferView(
            transfer.getId(),
            transfer.getFromAccountId(),
            transfer.getToAccountId(),
            transfer.getAmount().amount(),
            transfer.getAmount().currencyCode(),
            transfer.getCreatedAt()
        );
    }
}
