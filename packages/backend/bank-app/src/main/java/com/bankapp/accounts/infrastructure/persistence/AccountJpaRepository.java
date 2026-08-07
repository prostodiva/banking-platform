package com.bankapp.accounts.infrastructure.persistence;

import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountNumber;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AccountJpaRepository extends JpaRepository<Account, UUID> {
    boolean existsByAccountNumber(AccountNumber accountNumber);
}
