package com.bankapp.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * No {@code userId} field. The caller's identity comes from the validated access
 * token's {@code sub}, so there is nothing here for a client to claim.
 */
public record LogoutRequest(@NotBlank @Size(max = 200) String refreshToken) {
    @Override
    public String toString() {
        return "LogoutRequest[REDACTED]";
    }
}
