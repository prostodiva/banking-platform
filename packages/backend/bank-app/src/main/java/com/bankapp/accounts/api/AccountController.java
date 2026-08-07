package com.bankapp.accounts.api;

import com.bankapp.accounts.api.dto.AccountResponse;
import com.bankapp.accounts.api.dto.OpenAccountRequest;
import com.bankapp.accounts.application.getaccount.GetAccountHandler;
import com.bankapp.accounts.application.openaccount.OpenAccountCommand;
import com.bankapp.accounts.application.openaccount.OpenAccountHandler;
import com.bankapp.accounts.application.openaccount.OpenAccountResult;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/")
public class AccountController {

    private final OpenAccountHandler openAccount;
    private final GetAccountHandler getAccount;

    public AccountController(
        OpenAccountHandler openAccount,
        GetAccountHandler getAccount
    ) {
        this.openAccount = openAccount;
        this.getAccount = getAccount;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> open(
        @Valid @RequestBody OpenAccountRequest request
    ) {
        OpenAccountResult result = openAccount.handle(
            new OpenAccountCommand(
                request.ownerId(),
                request.type(),
                request.currencyCode()
            )
        );

        return ResponseEntity.created(
            URI.create("/api/accounts/" + result.id())
        ).body(AccountResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> get(@PathVariable UUID id) {
        return getAccount
            .handle(id)
            .map(view -> ResponseEntity.ok(AccountResponse.from(view)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
