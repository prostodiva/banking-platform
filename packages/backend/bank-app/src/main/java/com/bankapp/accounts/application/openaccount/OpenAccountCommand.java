package com.bankapp.accounts.application.openaccount;

import com.bankapp.accounts.domain.AccountType;
import java.util.UUID;

public record OpenAccountCommand(
    UUID ownerId,
    AccountType type,
    String currentCode
) {}
