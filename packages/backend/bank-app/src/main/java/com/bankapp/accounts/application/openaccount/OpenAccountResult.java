package com.bankapp.accounts.application.openaccount;

import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountNumber;
import com.bankapp.accounts.domain.AccountStatus;
import com.bankapp.accounts.domain.AccountType;
import java.math.BigDecimal;
import java.util.UUID;

public record OpenAccountResult(
    UUID id,
    AccountNumber accountNumber,
    AccountType type,
    AccountStatus status,
    BigDecimal balance,
    String currencyCode
) {
    public static OpenAccountResult from(Account account) {
        return new OpenAccountResult(
            account.getId(),
            account.getAccountNumber(),
            account.getType(),
            account.getStatus(),
            account.getBalance().amount(),
            account.getBalance().currencyCode()
        );
    }
}
