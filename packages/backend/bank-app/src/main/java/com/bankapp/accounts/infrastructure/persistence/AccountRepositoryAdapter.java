package com.bankapp.accounts.infrastructure.persistence;

import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class AccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository jpa;

    AccountRepositoryAdapter(AccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Account save(Account account) {
        return jpa.save(account);
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return jpa.existsByAccountNumber(accountNumber);
    }
}
