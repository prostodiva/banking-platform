package com.bankapp.accounts.application.closeaccount;

import com.bankapp.accounts.application.getaccount.AccountView;
import com.bankapp.accounts.application.port.DomainEventPublisher;
import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountRepository;
import com.bankapp.accounts.domain.events.AccountClosed;
import com.bankapp.accounts.domain.exceptions.AccountNotFoundException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloseAccountHandler {

    private final AccountRepository accounts;
    private final DomainEventPublisher events;

    public CloseAccountHandler(
        AccountRepository accounts,
        DomainEventPublisher events
    ) {
        this.accounts = accounts;
        this.events = events;
    }

    @Transactional
    public AccountView handle(CloseAccountCommand command) {
        Account account = accounts
            .findById(command.accountId())
            .orElseThrow(() -> new AccountNotFoundException(command.accountId()));

        account.close();
        events.publish(
            new AccountClosed(
                account.getId(),
                account.getOwnerId(),
                Instant.now()
            )
        );

        return AccountView.from(account);
    }
}
