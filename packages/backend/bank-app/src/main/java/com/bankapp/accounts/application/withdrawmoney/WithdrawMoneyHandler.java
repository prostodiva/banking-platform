package com.bankapp.accounts.application.withdrawmoney;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankapp.accounts.application.getaccount.AccountView;
import com.bankapp.accounts.application.port.DomainEventPublisher;
import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountRepository;
import com.bankapp.accounts.domain.events.MoneyWithdrawn;
import com.bankapp.accounts.domain.exceptions.AccountNotFoundException;
import com.bankapp.shared.domain.Money;

@Service
public class WithdrawMoneyHandler {

    private final AccountRepository accounts;
    private final DomainEventPublisher events;

    public WithdrawMoneyHandler(
        AccountRepository accounts,
        DomainEventPublisher events
    ) {
        this.accounts = accounts;
        this.events = events;
    }

    @Transactional
    public AccountView handle(WithdrawMoneyCommand command) {
        Account account = accounts
            .findById(command.accountId())
            .orElseThrow(() ->
                new AccountNotFoundException(command.accountId())
            );

            Money amount = new Money(command.amount(), command.currencyCode());
            account.withdraw(amount);
            accounts.save(account);
            events.publish(
                new MoneyWithdrawn(
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
