package com.bankapp.frauddetection.domain;

import java.util.List;
import java.util.UUID;

public interface FraudAlertRepository {
    FraudAlert save(FraudAlert alert);
    boolean existsByTransferId(UUID transferId);
    List<FraudAlert> findByFromAccountId(UUID fromAccountId);
}
