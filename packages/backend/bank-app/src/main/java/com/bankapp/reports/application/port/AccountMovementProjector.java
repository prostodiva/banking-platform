package com.bankapp.reports.application.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published inbound port (ADR-003 decision 2): accounts reports each
 * completed deposit and withdrawal here, so reports can project it into
 * a transaction entry without depending on accounts internals.
 */
public interface AccountMovementProjector {

    void projectDeposit(
        UUID eventId, UUID accountId, BigDecimal amount, String currencyCode, Instant occurredAt);

    void projectWithdrawal(
        UUID eventId, UUID accountId, BigDecimal amount, String currencyCode, Instant occurredAt);
}
