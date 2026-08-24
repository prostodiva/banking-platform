package com.bankapp.payments.domain;

import com.bankapp.shared.domain.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A movement of money that <em>already happened</em> — not a workflow (ADR-003 §4).
 * There is no status and no PENDING/FAILED: in one synchronous transaction the row
 * either committed, and the transfer succeeded, or it rolled back and there is no row.
 *
 * <p>Written once and never updated, which is why the table carries no version
 * column — there is no lost update for optimistic locking to catch.
 *
 * <p>Account ids are plain UUIDs with no foreign key: accounts is a different
 * bounded context, reached by id only.
 */
@Entity
@Table(name = "transfers")
public class Transfer {

    /** Matches varchar(64) in V4; a longer key is the client's bug, not a DB error. */
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 64;

    @Id
    private UUID id;

    @Column(name = "from_account_id", nullable = false, updatable = false)
    private UUID fromAccountId;

    @Column(name = "to_account_id", nullable = false, updatable = false)
    private UUID toAccountId;

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

    @Column(
        name = "idempotency_key",
        nullable = false,
        unique = true,
        updatable = false
    )
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Transfer() {
        // required by JPA; protected so application code can't create blank transfers
    }

    private Transfer(
        UUID fromAccountId,
        UUID toAccountId,
        Money amount,
        String idempotencyKey
    ) {
        if (fromAccountId == null) {
            throw new IllegalArgumentException("fromAccountId is required");
        }
        if (toAccountId == null) {
            throw new IllegalArgumentException("toAccountId is required");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException(
                "cannot transfer to the same account"
            );
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (!amount.isPositive()) {
            throw new IllegalArgumentException(
                "transfer amount must be positive"
            );
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        if (idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new IllegalArgumentException(
                "idempotencyKey must be at most " +
                MAX_IDEMPOTENCY_KEY_LENGTH +
                " characters"
            );
        }

        this.id = UUID.randomUUID();
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
    }

    /** The only way to create one: past tense, because the money has moved. */
    public static Transfer record(
        UUID fromAccountId,
        UUID toAccountId,
        Money amount,
        String idempotencyKey
    ) {
        return new Transfer(fromAccountId, toAccountId, amount, idempotencyKey);
    }

    /**
     * True when a replayed request carries the same movement this row already
     * records. A mismatch is key reuse across genuinely different requests — a
     * client bug the handler answers with 422 rather than replaying (ADR-003 §7).
     */
    public boolean records(UUID fromAccountId, UUID toAccountId, Money amount) {
        return (
            this.fromAccountId.equals(fromAccountId) &&
            this.toAccountId.equals(toAccountId) &&
            this.amount.equals(amount)
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getFromAccountId() {
        return fromAccountId;
    }

    public UUID getToAccountId() {
        return toAccountId;
    }

    public Money getAmount() {
        return amount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
