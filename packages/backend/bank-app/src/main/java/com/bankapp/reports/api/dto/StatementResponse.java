package com.bankapp.reports.api.dto;

import com.bankapp.reports.application.getstatement.StatementView;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StatementResponse(
    UUID accountId,
    String month,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    BigDecimal totalIn,
    BigDecimal totalOut,
    String currencyCode,
    List<TransactionEntryResponse> transactions
) {
    public static StatementResponse from(StatementView view) {
        return new StatementResponse(
            view.accountId(),
            view.month().toString(),
            view.openingBalance(),
            view.closingBalance(),
            view.totalIn(),
            view.totalOut(),
            view.currencyCode(),
            view.transactions().stream().map(TransactionEntryResponse::from).toList()
        );
    }
}
