package com.bankapp.payments.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Both accounts travel in the body: a transfer is a resource of its own, not a
 * sub-resource of either account.
 *
 * <p>The {@code Idempotency-Key} header is not part of this record — it is
 * transport, not the movement being requested, and the handler takes it
 * separately.
 *
 * <p>"fromAccountId must differ from toAccountId" is absent here too. Bean
 * Validation compares one field at a time; the rule is an invariant of Transfer
 * and refused there, which produces the same 400.
 */
public record TransferRequest(
    @NotNull UUID fromAccountId,
    @NotNull UUID toAccountId,
    @NotNull @Positive BigDecimal amount,
    @NotBlank @Size(min = 3, max = 3) String currencyCode
) {}
