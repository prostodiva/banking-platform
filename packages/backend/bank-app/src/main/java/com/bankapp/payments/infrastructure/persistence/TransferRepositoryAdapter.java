package com.bankapp.payments.infrastructure.persistence;

import com.bankapp.payments.domain.Transfer;
import com.bankapp.payments.domain.TransferRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class TransferRepositoryAdapter implements TransferRepository {

    private final TransferJpaRepository jpa;

    TransferRepositoryAdapter(TransferJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Transfer save(Transfer transfer) {
        return jpa.save(transfer);
    }

    @Override
    public Optional<Transfer> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Transfer> findByIdempotencyKey(String idempotencyKey) {
        return jpa.findByIdempotencyKey(idempotencyKey);
    }
}
