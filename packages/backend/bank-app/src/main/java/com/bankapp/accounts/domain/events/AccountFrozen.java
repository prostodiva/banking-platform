package com.bankapp.accounts.domain.events;

import java.time.Instant;
import java.util.UUID;

public record AccountFrozen(UUID accountId, UUID ownerId, Instant occurredAt) {}
