package com.bankapp.reports.domain;

import com.bankapp.shared.domain.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One money movement on one account, projected from a domain event (ADR-006).
 * A read-model row, not an aggregate: written once by a projection handler and
 * never updated. A transfer produces two entries — TRANSFER_OUT on the sender,
 * TRANSFER_IN on the receiver — sharing the transfer id as their sourceId.
 *
 * <p>The amount is always positive; direction lives in the type, never in a
 * signed amount, so statement totals are plain sums per type.
 */
@Entity
@Table(name = "transaction_entries")
public class TransactionEntry {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 20)
    private TransactionEntryType type;

    @Embedded
    @AttributeOverride(
        name = "amount",
        column = @Column(name = "amount", nullable = false, updatable = false)
    )
    @AttributeOverride(
        name = "currencyCode",
        column = @Column(
            name = "amount_currency",
            nullable = false,
            updatable = false,
            length = 3
        )
    )
    private Money amount;

    @Column(name = "counterparty_account_id", updatable = false)
    private UUID counterpartyAccountId;

    /**
     * Dedup key: the eventId for deposits/withdrawals, the transferId for
     * transfers. Not unique on its own — one transfer id legitimately appears
     * on two accounts. Uniqueness is the (account_id, source_id) pair, which
     * the V9 migration enforces.
     */
    @Column(name = "source_id", nullable = false, updatable = false)
    private UUID sourceId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected TransactionEntry() {
        // required by JPA; protected so application code can't create blank entries
    }

    private TransactionEntry(
        UUID accountId,
        TransactionEntryType type,
        Money amount,
        UUID counterpartyAccountId,
        UUID sourceId,
        Instant occurredAt
    ) {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("entry amount must be positive");
        }
        boolean transfer = type == TransactionEntryType.TRANSFER_IN
            || type == TransactionEntryType.TRANSFER_OUT;
        if (transfer && counterpartyAccountId == null) {
            throw new IllegalArgumentException(
                "counterpartyAccountId is required for transfers"
            );
        }
        if (!transfer && counterpartyAccountId != null) {
            throw new IllegalArgumentException(
                "counterpartyAccountId is only allowed for transfers"
            );
        }
        if (sourceId == null) {
            throw new IllegalArgumentException("sourceId is required");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }

        this.id = UUID.randomUUID();
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.counterpartyAccountId = counterpartyAccountId;
        this.sourceId = sourceId;
        this.occurredAt = occurredAt;
    }

    /** The only way to create one: an event is projected into a read-model row. */
    public static TransactionEntry project(
        UUID accountId,
        TransactionEntryType type,
        Money amount,
        UUID counterpartyAccountId,
        UUID sourceId,
        Instant occurredAt
    ) {
        return new TransactionEntry(
            accountId, type, amount, counterpartyAccountId, sourceId, occurredAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public TransactionEntryType getType() {
        return type;
    }

    public Money getAmount() {
        return amount;
    }

    public UUID getCounterpartyAccountId() {
        return counterpartyAccountId;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
