package com.bankapp.shared.domain;

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
}
