package com.bankapp.accounts.application.port;

import java.util.UUID;

import com.bankapp.shared.domain.Money;

public interface AccountLedger {

	void moveMoney(UUID fromAccountId, UUID toAccountId, Money amount);
}
