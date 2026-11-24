package com.bankapp.reports.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TransactionEntryRepository {

    TransactionEntry save(TransactionEntry entry);

    /**
     * The dedup fast path for projection handlers: a hit means this event was
     * already projected, so the handler skips instead of writing a second row.
     * The (account_id, source_id) unique constraint in V9 is the backstop for
     * two deliveries racing past this check.
     */
    boolean existsByAccountIdAndSourceId(UUID accountId, UUID sourceId);

    /** Entries before {@code instant}, chronological. Backs the statement's
     *  derived opening balance: the signed sum of everything before the month. */
    List<TransactionEntry> findByAccountIdAndOccurredAtBefore(UUID accountId, Instant instant);

    /** Entries in [{@code from}, {@code to}), chronological — statement order. */
    List<TransactionEntry> findByAccountIdAndOccurredAtBetweenOrderByOccurredAtAsc(
        UUID accountId, Instant from, Instant to);

    /** Entries in [{@code from}, {@code to}), newest first — history order. */
    List<TransactionEntry> findByAccountIdAndOccurredAtBetweenOrderByOccurredAtDesc(
        UUID accountId, Instant from, Instant to);
}
