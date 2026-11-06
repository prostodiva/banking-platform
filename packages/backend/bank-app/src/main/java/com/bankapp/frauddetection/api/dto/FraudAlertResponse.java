package com.bankapp.frauddetection.api.dto;

import com.bankapp.frauddetection.application.getfraudalerts.FraudAlertView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudAlertResponse(
    UUID id,
    UUID transferId,
    UUID fromAccountId,
    BigDecimal amount,
    String currencyCode,
    String reason,
    String status,
    Instant createdAt
) {
    public static FraudAlertResponse from(FraudAlertView view) {
        return new FraudAlertResponse(
            view.id(),
            view.transferId(),
            view.fromAccountId(),
            view.amount(),
            view.currencyCode(),
            view.reason(),
            view.status(),
            view.createdAt()
        );
    }
}
