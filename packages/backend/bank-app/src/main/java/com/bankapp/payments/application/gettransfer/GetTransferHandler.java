package com.bankapp.payments.application.gettransfer;

import com.bankapp.payments.domain.TransferRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetTransferHandler {

    private final TransferRepository transfers;

    public GetTransferHandler(TransferRepository transfers) {
        this.transfers = transfers;
    }

    @Transactional(readOnly = true)
    public Optional<TransferView> handle(UUID transferId) {
        return transfers.findById(transferId).map(TransferView::from);
    }
}
