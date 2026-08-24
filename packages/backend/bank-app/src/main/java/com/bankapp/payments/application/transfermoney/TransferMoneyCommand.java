package com.bankapp.payments.application.transfermoney;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferMoneyCommand(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String currencyCode, String idempotencyKey) {
}
