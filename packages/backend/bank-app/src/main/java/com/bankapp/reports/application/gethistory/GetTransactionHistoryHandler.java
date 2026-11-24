package com.bankapp.reports.application.gethistory;

import com.bankapp.accounts.application.port.AccountOwnershipQuery;
import com.bankapp.reports.application.shared.TransactionEntryView;
import com.bankapp.reports.domain.TransactionEntryRepository;
import com.bankapp.shared.domain.EntityNotFoundException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetTransactionHistoryHandler {

    private final TransactionEntryRepository entries;
    private final AccountOwnershipQuery ownership;

    public GetTransactionHistoryHandler(
        TransactionEntryRepository entries,
        AccountOwnershipQuery ownership
    ) {
        this.entries = entries;
        this.ownership = ownership;
    }

    @Transactional(readOnly = true)
    public List<TransactionEntryView> handle(GetTransactionHistoryQuery query) {
        if (query.from().isAfter(query.to())) {
            throw new IllegalArgumentException("from must not be after to");
        }
        boolean owner = ownership.ownerOf(query.accountId())
            .map(ownerId -> ownerId.equals(query.callerId()))
            .orElse(false);
        if (!owner) {
            throw new EntityNotFoundException("No account with id: " + query.accountId());
        }

        Instant from = query.from().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toExclusive = query.to().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return entries
            .findByAccountIdAndOccurredAtBetweenOrderByOccurredAtDesc(query.accountId(), from, toExclusive)
            .stream()
            .map(TransactionEntryView::from)
            .toList();
    }
}
