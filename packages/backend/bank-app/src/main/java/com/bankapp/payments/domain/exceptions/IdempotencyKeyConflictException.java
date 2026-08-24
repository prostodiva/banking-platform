package com.bankapp.payments.domain.exceptions;

import com.bankapp.shared.domain.UnprocessableRequestException;

/**
 * Same {@code Idempotency-Key}, different movement — a client bug (ADR-003 §7).
 * Replaying the first result would hide it while quietly not performing the
 * second transfer, so the request is refused instead.
 */
public class IdempotencyKeyConflictException extends UnprocessableRequestException {

    public IdempotencyKeyConflictException(String idempotencyKey) {
        super(
            "Idempotency-Key '" +
            idempotencyKey +
            "' was already used for a different transfer"
        );
    }
}
