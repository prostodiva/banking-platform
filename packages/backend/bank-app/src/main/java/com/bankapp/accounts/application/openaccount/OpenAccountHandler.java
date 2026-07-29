package com.bankapp.accounts.application.openaccount;

import com.bankapp.accounts.application.port.DomainEventPublisher;
import com.bankapp.accounts.domain.Account;
import com.bankapp.accounts.domain.AccountNumber;
import com.bankapp.accounts.domain.AccountRepository;
import com.bankapp.accounts.domain.event.AccountOpened;
import java.time.Instant;
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
        String accountNumber = uniqueAccountNumber();
        Account account = Account.open(
            command.ownerId(),
            command.type(),
            command.currentCode(),
            accountNumber
        );

        accounts.save(account);
        events.publish(
            new AccountOpened(
                account.getId(),
                account.getOwnerId(),
                Instant.now()
            )
        );

        return OpenAccountResult.from(account);
    }

    private String uniqueAccountNumber() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = AccountNumber.generate();
            if (!accounts.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException(
            "could not generate a unique account number"
        );
    }
}
