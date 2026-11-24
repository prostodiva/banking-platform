package com.bankapp.reports.application.getstatement;

import com.bankapp.accounts.application.port.AccountOwnershipQuery;
import com.bankapp.reports.application.shared.TransactionEntryView;
import com.bankapp.reports.domain.TransactionEntry;
import com.bankapp.reports.domain.TransactionEntryRepository;
import com.bankapp.reports.domain.TransactionEntryType;
import com.bankapp.shared.domain.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetMonthlyStatementHandler {

    private final TransactionEntryRepository entries;
    private final AccountOwnershipQuery ownership;

    public GetMonthlyStatementHandler(
        TransactionEntryRepository entries,
        AccountOwnershipQuery ownership
    ) {
        this.entries = entries;
        this.ownership = ownership;
    }

    @Transactional(readOnly = true)
    public StatementView handle(GetMonthlyStatementQuery query) {
        if (query.month().isAfter(YearMonth.now())) {
            throw new IllegalArgumentException("month must not be in the future");
        }
        requireOwner(query.accountId(), query.callerId());

        Instant monthStart = query.month().atDay(1)
            .atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant nextMonthStart = query.month().plusMonths(1).atDay(1)
            .atStartOfDay(ZoneOffset.UTC).toInstant();

        List<TransactionEntry> before = entries
            .findByAccountIdAndOccurredAtBefore(query.accountId(), monthStart);
        BigDecimal opening = BigDecimal.ZERO;
        for (TransactionEntry entry : before) {
            opening = opening.add(signedAmount(entry));
        }

        List<TransactionEntry> monthEntries = entries
            .findByAccountIdAndOccurredAtBetweenOrderByOccurredAtAsc(
                query.accountId(), monthStart, nextMonthStart);

        BigDecimal totalIn = BigDecimal.ZERO;
        BigDecimal totalOut = BigDecimal.ZERO;
        for (TransactionEntry entry : monthEntries) {
            if (isIn(entry.getType())) {
                totalIn = totalIn.add(entry.getAmount().amount());
            } else {
                totalOut = totalOut.add(entry.getAmount().amount());
            }
        }

        BigDecimal closing = opening.add(totalIn).subtract(totalOut);

        return new StatementView(
            query.accountId(),
            query.month(),
            opening,
            closing,
            totalIn,
            totalOut,
            currencyOf(monthEntries, before),
            monthEntries.stream().map(TransactionEntryView::from).toList()
        );
    }

    private void requireOwner(UUID accountId, UUID callerId) {
        boolean owner = ownership.ownerOf(accountId)
            .map(ownerId -> ownerId.equals(callerId))
            .orElse(false);
        if (!owner) {
            throw new EntityNotFoundException("No account with id: " + accountId);
        }
    }

    private BigDecimal signedAmount(TransactionEntry entry) {
        return isIn(entry.getType())
            ? entry.getAmount().amount()
            : entry.getAmount().amount().negate();
    }

    private boolean isIn(TransactionEntryType type) {
        return type == TransactionEntryType.DEPOSIT
            || type == TransactionEntryType.TRANSFER_IN;
    }

    private String currencyOf(List<TransactionEntry> monthEntries, List<TransactionEntry> before) {
        if (!monthEntries.isEmpty()) {
            return monthEntries.get(0).getAmount().currencyCode();
        }
        if (!before.isEmpty()) {
            return before.get(before.size() - 1).getAmount().currencyCode();
        }
        return null;
    }
}
