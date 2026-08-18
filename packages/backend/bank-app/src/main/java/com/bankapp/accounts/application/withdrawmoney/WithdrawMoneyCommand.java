package com.bankapp.accounts.application.withdrawmoney;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawMoneyCommand(UUID accountId, BigDecimal amount, String currencyCode) {}
