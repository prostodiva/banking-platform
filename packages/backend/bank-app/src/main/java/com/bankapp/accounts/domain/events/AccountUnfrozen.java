package com.bankapp.accounts.domain.events;

import java.time.Instant;
import java.util.UUID;

public record AccountUnfrozen(
    UUID ownerId,
    UUID accountId,
    Instant occurredAt
) {}
