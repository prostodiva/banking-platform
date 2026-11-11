package com.bankapp.notifications.domain;

import com.bankapp.shared.domain.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "alert_id", nullable = false, unique = true, updatable = false)
    private UUID alertId;

    @Column(name = "recipient_id", nullable = false, updatable = false)
    private UUID recipientId;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 20)
    private NotificationType type;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount",
                    column = @Column(name = "amount", nullable = false, precision = 19, scale = 4)),
            @AttributeOverride(name = "currencyCode",
                    column = @Column(name = "amount_currency", nullable = false, length = 3))
    })
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
    }

    private Notification(UUID alertId, UUID recipientId, UUID accountId, Money amount) {
        this.id = UUID.randomUUID();
        this.alertId = alertId;
        this.recipientId = recipientId;
        this.accountId = accountId;
        this.type = NotificationType.FRAUD_ALERT;
        this.amount = amount;
        this.createdAt = Instant.now();
    }

    public static Notification forFraudAlert(UUID alertId, UUID recipientId, UUID accountId, Money amount) {
        if (alertId == null) {
            throw new IllegalArgumentException("alertId is required");
        }
        if (recipientId == null) {
            throw new IllegalArgumentException("recipientId is required");
        }
        if (accountId == null) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        return new Notification(alertId, recipientId, accountId, amount);
    }

    public void markSent() {
        recordOutcome(NotificationStatus.SENT);
    }

    public void markFailed() {
        recordOutcome(NotificationStatus.FAILED);
    }

    private void recordOutcome(NotificationStatus outcome) {
        if (this.status != null) {
            throw new IllegalStateException("delivery outcome already recorded");
        }
        this.status = outcome;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public NotificationType getType() {
        return type;
    }

    public Money getAmount() {
        return amount;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
