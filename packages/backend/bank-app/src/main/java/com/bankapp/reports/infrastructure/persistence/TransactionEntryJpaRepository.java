package com.bankapp.reports.infrastructure.persistence;

import com.bankapp.reports.domain.TransactionEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TransactionEntryJpaRepository extends JpaRepository<TransactionEntry, UUID> {

    boolean existsByAccountIdAndSourceId(UUID accountId, UUID sourceId);

    List<TransactionEntry> findByAccountIdAndOccurredAtBefore(UUID accountId, Instant instant);

    List<TransactionEntry> findByAccountIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAsc(
        UUID accountId, Instant from, Instant to);

    List<TransactionEntry> findByAccountIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
        UUID accountId, Instant from, Instant to);
}
