package com.bankapp.accounts.infrastructure.ownership;

import com.bankapp.accounts.application.port.AccountOwnershipQuery;
import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the published ownership query by reading the accounts
 * aggregate. Read-only: it never mutates the account and publishes nothing.
 */
@Service
class AccountOwnershipQueryAdapter implements AccountOwnershipQuery {

    private final AccountRepository accounts;

    AccountOwnershipQueryAdapter(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> ownerOf(UUID accountId) {
        return accounts.findById(accountId).map(Account::getOwnerId);
    }
}
