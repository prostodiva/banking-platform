package com.bankapp.payments.application.transfermoney;

import com.bankapp.accounts.application.port.AccountLedger;
import com.bankapp.payments.application.port.DomainEventPublisher;
import com.bankapp.payments.domain.Transfer;
import com.bankapp.payments.domain.TransferRepository;
import com.bankapp.payments.domain.events.PaymentCompleted;
import com.bankapp.payments.domain.exceptions.IdempotencyKeyConflictException;
import com.bankapp.shared.domain.Money;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records a transfer and moves the money, once per idempotency key (ADR-003).
 *
 * <p>The whole use case is one transaction: the debit, the credit and the transfer
 * row commit together or not at all. Money never crosses the accounts boundary
 * here — {@link AccountLedger} is the only thing this context knows about accounts.
 */
@Service
public class TransferMoneyHandler {

    private final TransferRepository transfers;
    private final AccountLedger ledger;
    private final DomainEventPublisher events;

    public TransferMoneyHandler(
        TransferRepository transfers,
        AccountLedger ledger,
        DomainEventPublisher events
    ) {
        this.transfers = transfers;
        this.ledger = ledger;
        this.events = events;
    }

    @Transactional
    public TransferMoneyResult handle(TransferMoneyCommand command) {
        Money amount = new Money(command.amount(), command.currencyCode());

        // Replay path: this key already ran. Return what it returned — no second
        // movement, and no second event (ADR-003 §5).
        Optional<Transfer> alreadyRecorded = transfers.findByIdempotencyKey(
            command.idempotencyKey()
        );
        if (alreadyRecorded.isPresent()) {
            return replay(alreadyRecorded.get(), command, amount);
        }

        Transfer transfer = Transfer.record(
            command.fromAccountId(),
            command.toAccountId(),
            amount,
            command.idempotencyKey()
        );

        // Money first: an unknown account, a frozen one or insufficient funds all
        // refuse here, and the transfer row is never written for a movement that
        // did not happen.
        ledger.moveMoney(
            command.fromAccountId(),
            command.toAccountId(),
            amount
        );
        transfers.save(transfer);

        events.publish(
            new PaymentCompleted(
                transfer.getId(),
                transfer.getFromAccountId(),
                transfer.getToAccountId(),
                transfer.getAmount().amount(),
                transfer.getAmount().currencyCode(),
                transfer.getIdempotencyKey(),
                Instant.now()
            )
        );

        return TransferMoneyResult.from(transfer);
    }

    /**
     * A retry must be indistinguishable from the original — same 201, same body.
     * A key reused for a <em>different</em> movement is a client bug, not a retry,
     * and gets 422 rather than someone else's result (ADR-003 §7).
     */
    private TransferMoneyResult replay(
        Transfer recorded,
        TransferMoneyCommand command,
        Money amount
    ) {
        if (
            !recorded.records(
                command.fromAccountId(),
                command.toAccountId(),
                amount
            )
        ) {
            throw new IdempotencyKeyConflictException(command.idempotencyKey());
        }
        return TransferMoneyResult.from(recorded);
    }
}
