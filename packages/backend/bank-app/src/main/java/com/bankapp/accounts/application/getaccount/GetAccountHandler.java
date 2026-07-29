package com.bankapp.accounts.application.getaccount;

import com.bankapp.accounts.domain.AccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAccountHandler {

    private final AccountRepository accounts;

    public GetAccountHandler(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional
    public Optional<AccountView> handle(UUID accountId) {
        return accounts.findById(accountId).map(AccountView::from);
    }
}
