package com.bankapp.shared.domain;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;

@Embeddable
public record Money(BigDecimal amount, String currencyCode) {
    /** Storage scale — matches the numeric(19,4) columns money is persisted in. */
    private static final int SCALE = 4;

    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (currencyCode == null || currencyCode.length() != 3) {
            throw new IllegalArgumentException(
                "currencyCode must be a 3-letter ISO code"
            );
        }
        if (amount.scale() > SCALE) {
            throw new IllegalArgumentException(
                "amount supports at most 4 decimal places"
            );
        }
        // Normalize scale so equal amounts are equal objects: BigDecimal.equals
        // compares scale, so without this 0 != 0.0000 and Money would break the
        // core value-object contract (and every DB round-trip comparison).
        amount = amount.setScale(SCALE, RoundingMode.HALF_EVEN);
        currencyCode = currencyCode.toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "currencyCode must be a valid ISO 4217 code: " + currencyCode,
                e
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

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
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
