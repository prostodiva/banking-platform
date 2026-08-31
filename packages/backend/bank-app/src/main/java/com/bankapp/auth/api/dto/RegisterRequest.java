
package com.bankapp.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * There is no role field, and adding one would be a privilege-escalation
 * endpoint: this request is public, so every field in it is attacker-controlled
 * (ADR-004 decision 5).
 */
public record RegisterRequest(
    @NotBlank @Email @Size(max = 320) String email,
    @NotBlank @Size(max = 200) String fullName,
    @NotBlank @Size(min = 8, max = 72) String password
) {
    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + "]";
    }
}
