package com.bankapp.reports.application.getstatement;

import com.bankapp.reports.application.shared.TransactionEntryView;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public record StatementView(
    UUID accountId,
    YearMonth month,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    BigDecimal totalIn,
    BigDecimal totalOut,
    String currencyCode,
    List<TransactionEntryView> transactions
) {}
