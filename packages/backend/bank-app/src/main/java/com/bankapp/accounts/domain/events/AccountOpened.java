package com.bankapp.accounts.domain.events;

import java.time.Instant;
import java.util.UUID;

public record AccountOpened(UUID accountId, UUID ownerId, Instant occurredAt) {}
