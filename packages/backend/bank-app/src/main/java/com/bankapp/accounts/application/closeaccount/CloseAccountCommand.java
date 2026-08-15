package com.bankapp.accounts.application.closeaccount;

import java.util.UUID;

public record CloseAccountCommand(UUID accountId) {}
