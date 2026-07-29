package com.bankapp.accounts.domain;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Account save(Account amount);
    Optional<Account> findById(UUID id);
    boolean existsByAccountNumber(String accountNumber);
}
