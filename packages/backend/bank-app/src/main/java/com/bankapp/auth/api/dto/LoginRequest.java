package com.bankapp.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Validates less than RegisterRequest, on purpose: every rule this endpoint
 * states is a rule an attacker learns for free. No {@code @Email}, no minimum
 * password length — a credential that cannot match any row is refused by being
 * wrong, not by being malformed.
 *
 * <p>The generous maximum is not a policy statement. It is there so the endpoint
 * cannot be handed a megabyte of JSON to parse.
 */
public record LoginRequest(
    @NotBlank @Size(max = 320) String email,
    @NotBlank @Size(max = 1000) String password
) {
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}
