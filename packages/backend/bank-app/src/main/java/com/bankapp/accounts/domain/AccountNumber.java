package com.bankapp.accounts.domain;

import java.security.SecureRandom;
import java.util.regex.Pattern;

public record AccountNumber(String value) {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern FORMAT = Pattern.compile("[1-9]\\d{9}");

    public AccountNumber {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "account number must be 10 digits: " + value
            );
        }
    }

    public static AccountNumber generate() {
        StringBuilder digits = new StringBuilder();
        digits.append(1 + RANDOM.nextInt(9));
        for (int i = 1; i < 10; i++) digits.append(RANDOM.nextInt(10));
        return new AccountNumber(digits.toString());
    }
}
