package com.bankapp.reports.application.projectpayment;

import com.bankapp.reports.domain.TransactionEntry;
import com.bankapp.reports.domain.TransactionEntryRepository;
import com.bankapp.reports.domain.TransactionEntryType;
import com.bankapp.shared.domain.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectPaymentHandler {

    private final TransactionEntryRepository entries;

    public ProjectPaymentHandler(TransactionEntryRepository entries) {
        this.entries = entries;
    }

    @Transactional
    public void handle(ProjectPaymentCommand command) {
        Money amount = new Money(command.amount(), command.currencyCode());

        if (!entries.existsByAccountIdAndSourceId(command.fromAccountId(), command.transferId())) {
            entries.save(TransactionEntry.project(
                command.fromAccountId(),
                TransactionEntryType.TRANSFER_OUT,
                amount,
                command.toAccountId(),
                command.transferId(),
                command.occurredAt()
            ));
        }

        if (!entries.existsByAccountIdAndSourceId(command.toAccountId(), command.transferId())) {
            entries.save(TransactionEntry.project(
                command.toAccountId(),
                TransactionEntryType.TRANSFER_IN,
                amount,
                command.fromAccountId(),
                command.transferId(),
                command.occurredAt()
            ));
        }
    }
}
