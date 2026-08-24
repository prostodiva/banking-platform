package com.bankapp.payments.domain.exceptions;

import com.bankapp.shared.domain.EntityNotFoundException;
import java.util.UUID;

public class TransferNotFoundException extends EntityNotFoundException {

    public TransferNotFoundException(UUID transferId) {
        super("No transfer with id: " + transferId);
    }
}
