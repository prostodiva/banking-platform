package com.bankapp.accounts.application.openaccount;

import com.bankapp.accounts.application.port.DomainEventPublisher;
import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountNumber;
import com.bankapp.accounts.domain.AccountRepository;
import com.bankapp.accounts.domain.events.AccountOpened;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenAccountHandler {

    private final AccountRepository accounts;
    private final DomainEventPublisher events;

    public OpenAccountHandler(
        AccountRepository accounts,
        DomainEventPublisher events
    ) {
        this.accounts = accounts;
        this.events = events;
    }

    @Transactional
    public OpenAccountResult handle(OpenAccountCommand command) {
        AccountNumber accountNumber = uniqueAccountNumber();
        Account account = Account.open(
            command.ownerId(),
            command.type(),
            command.currencyCode(),
            accountNumber
        );

        accounts.save(account);
        events.publish(
            new AccountOpened(
                account.getId(),
                account.getOwnerId(),
                account.getCreatedAt()
            )
        );

        return OpenAccountResult.from(account);
    }

    private AccountNumber uniqueAccountNumber() {
        for (int attempt = 0; attempt < 5; attempt++) {
            AccountNumber candidate = AccountNumber.generate();
            if (!accounts.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException(
            "could not generate a unique account number"
        );
    }
}
