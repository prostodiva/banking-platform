package com.bankapp.frauddetection.infrastructure.persistence;

import com.bankapp.frauddetection.domain.FraudAlert;
import com.bankapp.frauddetection.domain.FraudAlertRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class FraudAlertRepositoryAdapter implements FraudAlertRepository {

    private final FraudAlertJpaRepository jpa;

    FraudAlertRepositoryAdapter(FraudAlertJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public FraudAlert save(FraudAlert alert) {
        return jpa.save(alert);
    }

    @Override
    public boolean existsByTransferId(UUID transferId) {
        return jpa.existsByTransferId(transferId);
    }

    @Override
    public List<FraudAlert> findByFromAccountId(UUID fromAccountId) {
        return jpa.findByFromAccountId(fromAccountId);
    }
}
