package com.bankapp.accounts.application.getaccount;

import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountNumber;
import com.bankapp.accounts.domain.AccountStatus;
import com.bankapp.accounts.domain.AccountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountView(
    UUID id,
    AccountNumber accountNumber,
    UUID ownerId,
    AccountType type,
    AccountStatus status,
    BigDecimal balance,
    String currencyCode,
    Instant createdAt
) {
    public static AccountView from(Account account) {
        return new AccountView(
            account.getId(),
            account.getAccountNumber(),
            account.getOwnerId(),
            account.getType(),
            account.getStatus(),
            account.getBalance().amount(),
            account.getBalance().currencyCode(),
            account.getCreatedAt()
        );
    }
}
