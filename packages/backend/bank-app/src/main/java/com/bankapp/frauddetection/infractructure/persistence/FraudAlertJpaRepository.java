package com.bankapp.frauddetection.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.bankapp.frauddetection.domain.FraudAlert;
import java.util.List;
import java.util.UUID;

interface FraudAlertJpaRepository extends JpaRepository<FraudAlert, UUID> {
    boolean existsByTransferId(UUID transferId);
    List<FraudAlert> findByFromAccountId(UUID fromAccountId);
}
