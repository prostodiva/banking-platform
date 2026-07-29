package com.bankapp.accounts.domain;

import java.security.SecureRandom;

public final class AccountNumber {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int LENGTH = 10;

    private AccountNumber() {}

    public static String generate() {
        StringBuilder digits = new StringBuilder(LENGTH);
        digits.append(1 + RANDOM.nextInt(9));
        for (int i = 1; i < LENGTH; i++) {
            digits.append(RANDOM.nextInt(10));
        }

        return digits.toString();
    }
}
