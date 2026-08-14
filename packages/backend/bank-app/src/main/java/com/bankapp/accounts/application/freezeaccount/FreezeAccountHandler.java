package com.bankapp.accounts.application.freezeaccount;

import com.bankapp.accounts.application.getaccount.AccountView;
import com.bankapp.accounts.application.port.DomainEventPublisher;
import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountRepository;
import com.bankapp.accounts.domain.events.AccountFrozen;
import com.bankapp.accounts.domain.exceptions.AccountNotFoundException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FreezeAccountHandler {

    private final AccountRepository accounts;
    private final DomainEventPublisher events;

    public FreezeAccountHandler(
        AccountRepository accounts,
        DomainEventPublisher events
    ) {
        this.accounts = accounts;
        this.events = events;
    }

    @Transactional
    public AccountView handle(FreezeAccountCommand command) {
        Account account = accounts
            .findById(command.accountId())
            .orElseThrow(() ->
                new AccountNotFoundException(command.accountId())
            );

        account.freeze();
        accounts.save(account);
        events.publish(
            new AccountFrozen(
                account.getId(),
                account.getOwnerId(),
                Instant.now()
            )
        );

        return AccountView.from(account);
    }
}
