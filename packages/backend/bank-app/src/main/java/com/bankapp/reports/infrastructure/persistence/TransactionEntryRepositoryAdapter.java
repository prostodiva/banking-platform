package com.bankapp.reports.infrastructure.persistence;

import com.bankapp.reports.domain.TransactionEntry;
import com.bankapp.reports.domain.TransactionEntryRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class TransactionEntryRepositoryAdapter implements TransactionEntryRepository {

    private final TransactionEntryJpaRepository jpa;

    TransactionEntryRepositoryAdapter(TransactionEntryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public TransactionEntry save(TransactionEntry entry) {
        return jpa.save(entry);
    }

    @Override
    public boolean existsByAccountIdAndSourceId(UUID accountId, UUID sourceId) {
        return jpa.existsByAccountIdAndSourceId(accountId, sourceId);
    }

    @Override
    public List<TransactionEntry> findByAccountIdAndOccurredAtBefore(UUID accountId, Instant instant) {
        return jpa.findByAccountIdAndOccurredAtBefore(accountId, instant);
    }

    @Override
    public List<TransactionEntry> findByAccountIdAndOccurredAtBetweenOrderByOccurredAtAsc(
        UUID accountId, Instant from, Instant to) {
        return jpa.findByAccountIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAsc(
            accountId, from, to);
    }

    @Override
    public List<TransactionEntry> findByAccountIdAndOccurredAtBetweenOrderByOccurredAtDesc(
        UUID accountId, Instant from, Instant to) {
        return jpa.findByAccountIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
            accountId, from, to);
    }
}
