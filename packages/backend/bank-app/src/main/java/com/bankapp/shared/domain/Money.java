package com.bankapp.shared.domain;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public record Money(BigDecimal amount, String currencyCode) {
    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (currencyCode == null || currencyCode.length() != 3) {
            throw new IllegalArgumentException(
                "currencyCode must be a 3-letter ISO code"
            );
        }
        if (amount.scale() > 4) {
            throw new IllegalArgumentException(
                "amount supports at most 4 decimal places"
            );
        }
    }

    public static Money zero(String currencyCode) {
        return new Money(BigDecimal.ZERO, currencyCode);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currencyCode);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currencyCode);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currencyCode.equals(other.currencyCode)) {
            throw new IllegalArgumentException(
                "currency mismatch: " +
                    currencyCode +
                    " vs " +
                    other.currencyCode
            );
        }
    }
}
