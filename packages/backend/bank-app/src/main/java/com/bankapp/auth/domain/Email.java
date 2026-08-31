package com.bankapp.auth.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {
    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_LENGTH = 320;

    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        // Normalize before validating and before storing. Without this,
        // "Ann@Example.com" and "ann@example.com" are two rows, the unique index
        // allows both, and which one you can log in as depends on how you typed it.
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("email must be at most 320 characters");
        }
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("email is not a valid address: " + value);
        }
    }
}
