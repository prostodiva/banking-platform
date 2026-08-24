package com.bankapp.payments.api;

import com.bankapp.payments.api.dto.TransferRequest;
import com.bankapp.payments.api.dto.TransferResponse;
import com.bankapp.payments.application.gettransfer.GetTransferHandler;
import com.bankapp.payments.application.transfermoney.TransferMoneyCommand;
import com.bankapp.payments.application.transfermoney.TransferMoneyHandler;
import com.bankapp.payments.application.transfermoney.TransferMoneyResult;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/transfers")
public class TransferController {

    private final TransferMoneyHandler transferMoney;
    private final GetTransferHandler getTransfer;

    public TransferController(
        TransferMoneyHandler transferMoney,
        GetTransferHandler getTransfer
    ) {
        this.transferMoney = transferMoney;
        this.getTransfer = getTransfer;
    }

    /**
     * Both accounts travel in the body — a transfer is a resource of its own,
     * not a sub-resource of either account.
     *
     * <p>{@code Idempotency-Key} is required: Spring answers a missing one with
     * 400 before this method runs. A replayed key returns this same 201 with the
     * original body, which is what lets a client retry blindly (ADR-003 §6).
     */
    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody TransferRequest request
    ) {
        TransferMoneyResult result = transferMoney.handle(
            new TransferMoneyCommand(
                request.fromAccountId(),
                request.toAccountId(),
                request.amount(),
                request.currencyCode(),
                idempotencyKey
            )
        );

        return ResponseEntity.created(
            URI.create("/api/payments/transfers/" + result.id())
        ).body(TransferResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> get(@PathVariable UUID id) {
        return getTransfer
            .handle(id)
            .map(view -> ResponseEntity.ok(TransferResponse.from(view)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
