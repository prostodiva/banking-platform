package com.bankapp.accounts.api.dto;

import com.bankapp.accounts.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record OpenAccountRequest(
    @NotNull UUID ownerId,
    @NotNull AccountType type,
    @NotBlank @Size(min = 3, max = 3) String currencyCode
) {}
