package com.bankapp.frauddetection.domain;

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
import java.util.Objects;
import java.util.UUID;

/**
 * A suspicion that was raised about a committed transfer — detection only.
 * Creating an alert never blocks, reverses, or mutates the transfer it
 * refers to.
 *
 * <p>One alert per transfer: {@code transfer_id} is unique, so a duplicate
 * {@code PaymentCompleted} delivery cannot create a second alert for the
 * same payment (FR-FRAUD-01, AC3).
 *
 * <p>Account ids are plain UUIDs with no foreign key: accounts and payments
 * are different bounded contexts, reached by id only.
 *
 * <p>Immutable in this slice — the only state is {@code RAISED}.
 * Acknowledge/dismiss transitions arrive with the review slice.
 */
@Entity
@Table(name = "fraud_alerts")
public class FraudAlert {

    @Id
    private UUID id;

    @Column(name = "transfer_id", nullable = false, unique = true, updatable = false)
    private UUID transferId;

    @Column(name = "from_account_id", nullable = false, updatable = false)
    private UUID fromAccountId;

    @Embedded
    @AttributeOverride(
        name = "amount",
        column = @Column(name = "amount", nullable = false, updatable = false)
    )
    @AttributeOverride(
        name = "currencyCode",
        column = @Column(name = "amount_currency", nullable = false, updatable = false, length = 3)
    )
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, updatable = false)
    private FraudAlertReason reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FraudAlertStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FraudAlert() {
        // JPA
    }

    private FraudAlert(UUID transferId, UUID fromAccountId, Money amount) {
        this.id = UUID.randomUUID();
        this.transferId = transferId;
        if (transferId == null) {
            throw new IllegalArgumentException("transferId is required");
        }
        this.fromAccountId = fromAccountId;
        if (fromAccountId == null) {
            throw new IllegalArgumentException("fromAccountId is required");
        }
        this.amount = amount;
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        this.reason = FraudAlertReason.LARGE_PAYMENT;
        this.status = FraudAlertStatus.RAISED;
        this.createdAt = Instant.now();
    }

    public static FraudAlert raise(UUID transferId, UUID fromAccountId, Money amount) {
        return new FraudAlert(transferId, fromAccountId, amount);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public UUID getFromAccountId() {
        return fromAccountId;
    }

    public Money getAmount() {
        return amount;
    }

    public FraudAlertReason getReason() {
        return reason;
    }

    public FraudAlertStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
