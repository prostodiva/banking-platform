package com.bankapp.accounts.domain;

import com.bankapp.shared.domain.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Column(
        name = "account_number",
        nullable = false,
        unique = true,
        updatable = false
    )
    private AccountNumber accountNumber;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Embedded
    @AttributeOverride(
        name = "amount",
        column = @Column(name = "balance_amount", nullable = false)
    )
    @AttributeOverride(
        name = "currencyCode",
        column = @Column(
            name = "balance_currency",
            nullable = false,
            length = 3
        )
    )
    private Money balance;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Account() {
        // required by JPA; protected so application code can't create blank accounts
    }

    private Account(
        UUID ownerId,
        AccountType type,
        String currencyCode,
        AccountNumber accountNumber
    ) {
        if (ownerId == null) {
            throw new IllegalArgumentException("ownerId is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }

        this.id = UUID.randomUUID();
        this.accountNumber = accountNumber;
        this.ownerId = ownerId;
        this.type = type;
        this.status = AccountStatus.ACTIVE;
        this.balance = Money.zero(currencyCode);
        this.createdAt = Instant.now();
    }

    public static Account open(
        UUID ownerId,
        AccountType type,
        String currencyCode,
        AccountNumber accountNumber
    ) {
        return new Account(ownerId, type, currencyCode, accountNumber);
    }

    public UUID getId() {
        return id;
    }

    public AccountNumber getAccountNumber() {
        return accountNumber;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public AccountType getType() {
        return type;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Money getBalance() {
        return balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
