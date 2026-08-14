package com.bankapp.accounts.application.unfreezeaccount;

import com.bankapp.accounts.application.getaccount.AccountView;
import com.bankapp.accounts.application.port.DomainEventPublisher;
import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountRepository;
import com.bankapp.accounts.domain.events.AccountUnfrozen;
import com.bankapp.accounts.domain.exceptions.AccountNotFoundException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnfreezeAccountHandler {

    private final AccountRepository accounts;
    private final DomainEventPublisher events;

    public UnfreezeAccountHandler(
        AccountRepository accounts,
        DomainEventPublisher events
    ) {
        this.accounts = accounts;
        this.events = events;
    }

    @Transactional
    public AccountView handle(UnfreezeAccountCommand command) {
        Account account = accounts
            .findById(command.accountId())
            .orElseThrow(() ->
                new AccountNotFoundException(command.accountId())
            );

        account.unfreeze();
        accounts.save(account);
        events.publish(
            new AccountUnfrozen(
                account.getId(),
                account.getOwnerId(),
                Instant.now()
            )
        );

        return AccountView.from(account);
    }
}
