package com.bankapp.reports.application.projectaccountmovement;

import com.bankapp.reports.application.port.AccountMovementProjector;
import com.bankapp.reports.domain.TransactionEntry;
import com.bankapp.reports.domain.TransactionEntryRepository;
import com.bankapp.reports.domain.TransactionEntryType;
import com.bankapp.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectAccountMovementHandler implements AccountMovementProjector {

    private final TransactionEntryRepository entries;

    public ProjectAccountMovementHandler(TransactionEntryRepository entries) {
        this.entries = entries;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void projectDeposit(
        UUID eventId, UUID accountId, BigDecimal amount, String currencyCode, Instant occurredAt
    ) {
        project(eventId, accountId, TransactionEntryType.DEPOSIT, amount, currencyCode, occurredAt);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void projectWithdrawal(
        UUID eventId, UUID accountId, BigDecimal amount, String currencyCode, Instant occurredAt
    ) {
        project(eventId, accountId, TransactionEntryType.WITHDRAWAL, amount, currencyCode, occurredAt);
    }

    private void project(
        UUID eventId, UUID accountId, TransactionEntryType type,
        BigDecimal amount, String currencyCode, Instant occurredAt
    ) {
        if (entries.existsByAccountIdAndSourceId(accountId, eventId)) {
            return;
        }
        entries.save(TransactionEntry.project(
            accountId,
            type,
            new Money(amount, currencyCode),
            null,
            eventId,
            occurredAt
        ));
    }
}
