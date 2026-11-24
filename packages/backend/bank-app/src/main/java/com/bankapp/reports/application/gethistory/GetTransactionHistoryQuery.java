package com.bankapp.reports.application.gethistory;

import java.time.LocalDate;
import java.util.UUID;

public record GetTransactionHistoryQuery(
    UUID accountId,
    UUID callerId,
    LocalDate from,
    LocalDate to
) {}
