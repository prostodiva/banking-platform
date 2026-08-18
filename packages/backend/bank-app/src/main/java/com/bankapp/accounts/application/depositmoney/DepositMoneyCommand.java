package com.bankapp.accounts.application.depositmoney;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositMoneyCommand(UUID accountId, BigDecimal amount, String currencyCode) {
}
