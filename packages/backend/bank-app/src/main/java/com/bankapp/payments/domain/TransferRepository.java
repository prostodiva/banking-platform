package com.bankapp.payments.domain;

import java.util.Optional;
import java.util.UUID;

public interface TransferRepository {
    Transfer save(Transfer transfer);

    /** Backs FR-PAY-02, {@code GET /api/payments/transfers/{id}}. */
    Optional<Transfer> findById(UUID id);

    /**
     * The idempotency fast path: a hit means this request already ran, so the
     * caller replays the stored result instead of moving money again (ADR-003 §6).
     *
     * <p>This is the only lookup by key. The amended §6 explains why there is no
     * second one after a unique-index violation — that re-read cannot run in a
     * transaction already marked rollback-only.
     */
    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);
}
