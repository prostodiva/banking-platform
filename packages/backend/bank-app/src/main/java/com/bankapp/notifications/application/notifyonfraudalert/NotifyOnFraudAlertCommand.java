package com.bankapp.notifications.application.notifyonfraudalert;

import java.math.BigDecimal;
import java.util.UUID;

public record NotifyOnFraudAlertCommand(
        UUID alertId,
        UUID fromAccountId,
        BigDecimal amount,
        String currencyCode
) {
}
