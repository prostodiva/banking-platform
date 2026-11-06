package com.bankapp.frauddetection.application.getfraudalerts;

import com.bankapp.frauddetection.domain.FraudAlert;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudAlertView(
    UUID id,
    UUID transferId,
    UUID fromAccountId,
    BigDecimal amount,
    String currencyCode,
    String reason,
    String status,
    Instant createdAt
) {
    public static FraudAlertView from(FraudAlert alert) {
        return new FraudAlertView(
            alert.getId(),
            alert.getTransferId(),
            alert.getFromAccountId(),
            alert.getAmount().amount(),
            alert.getAmount().currencyCode(),
            alert.getReason().name(),
            alert.getStatus().name(),
            alert.getCreatedAt()
        );
    }
}
