package com.bankapp.accounts.domain.exceptions;

import com.bankapp.shared.domain.EntityNotFoundException;
import java.util.UUID;

public class AccountNotFoundException extends EntityNotFoundException {

    public AccountNotFoundException(UUID accountId) {
        super("No account with id: " + accountId);
    }
}
