package com.bankapp.accounts.domain.event;

import java.time.Instant;
import java.util.UUID;

public record AccountOpened(UUID accountId, UUID ownerId, Instant occurredAt) {}
