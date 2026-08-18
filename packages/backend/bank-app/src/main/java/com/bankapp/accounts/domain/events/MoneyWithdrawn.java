package com.bankapp.accounts.domain.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MoneyWithdrawn(UUID accountId, UUID ownerId, BigDecimal amount, String currencyCode, Instant occurredAt) {}
