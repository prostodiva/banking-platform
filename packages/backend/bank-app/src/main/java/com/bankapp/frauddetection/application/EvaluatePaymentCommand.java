package com.bankapp.frauddetection.application.evaluatepayment;

import java.math.BigDecimal;
import java.util.UUID;

public record EvaluatePaymentCommand(
    UUID transferId,
    UUID fromAccountId,
    BigDecimal amount,
    String currencyCode
) {}
