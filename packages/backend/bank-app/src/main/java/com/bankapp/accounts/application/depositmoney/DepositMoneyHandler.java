package com.bankapp.accounts.application.depositmoney;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankapp.accounts.application.getaccount.AccountView;
import com.bankapp.accounts.application.port.DomainEventPublisher;
import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountRepository;
import com.bankapp.accounts.domain.events.MoneyDeposited;
import com.bankapp.accounts.domain.exceptions.AccountNotFoundException;
import com.bankapp.shared.domain.Money;

@Service
public class DepositMoneyHandler {

    private final AccountRepository accounts;
    private final DomainEventPublisher events;

    public DepositMoneyHandler(AccountRepository accounts, DomainEventPublisher events) {
        this.accounts = accounts;
        this.events = events;
    }

    @Transactional
    public AccountView handle(DepositMoneyCommand command) {
        Account account = accounts
            .findById(command.accountId())
            .orElseThrow(() ->
                new AccountNotFoundException(command.accountId())
            );

        Money amount = new Money(command.amount(), command.currencyCode());
        account.deposit(amount);
        accounts.save(account);
        events.publish(
            new MoneyDeposited(
                account.getId(),
                account.getOwnerId(),
                amount.amount(),
                amount.currencyCode(),
                Instant.now()
            )
        );


        return AccountView.from(account);
    }
}
