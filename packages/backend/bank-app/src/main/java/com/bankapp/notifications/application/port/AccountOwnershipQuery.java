package com.bankapp.accounts.application.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Published query port (ADR-005): who owns this account?
 *
 * <p>Returns empty when the account does not exist. A query port reports
 * "not found" as data, not as an exception — the consumer decides what an
 * unknown account means (notifications: log and skip, never fail).
 */
public interface AccountOwnershipQuery {

    Optional<UUID> ownerOf(UUID accountId);
}
