package com.bankapp.auth.domain.events;

import java.time.Instant;
import java.util.UUID;

public record UserRegistered(UUID userId, Instant occurredAt) {}
