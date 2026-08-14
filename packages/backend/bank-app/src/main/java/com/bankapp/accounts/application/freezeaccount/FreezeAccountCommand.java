package com.bankapp.accounts.application.freezeaccount;

import java.util.UUID;

public record FreezeAccountCommand(UUID accountId) {}
