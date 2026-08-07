package com.bankapp.accounts.api.dto;

import com.bankapp.accounts.application.getaccount.AccountView;
import com.bankapp.accounts.application.openaccount.OpenAccountResult;
import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
    UUID id,
    String accountNumber,
    String type,
    String status,
    BigDecimal balance,
    String currencyCode
) {
    public static AccountResponse from(OpenAccountResult result) {
        return new AccountResponse(
            result.id(),
            result.accountNumber().value(),
            result.type().name(),
            result.status().name(),
            result.balance(),
            result.currencyCode()
        );
    }

    public static AccountResponse from(AccountView view) {
        return new AccountResponse(
            view.id(),
            view.accountNumber().value(),
            view.type().name(),
            view.status().name(),
            view.balance(),
            view.currencyCode()
        );
    }
}
