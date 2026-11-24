package com.bankapp.reports.application.getstatement;

import java.time.YearMonth;
import java.util.UUID;

public record GetMonthlyStatementQuery(
    UUID accountId,
    UUID callerId,
    YearMonth month
) {}
