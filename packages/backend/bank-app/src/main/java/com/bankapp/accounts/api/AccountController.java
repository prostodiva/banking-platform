package com.bankapp.accounts.api;

import com.bankapp.accounts.api.dto.AccountResponse;
import com.bankapp.accounts.api.dto.AmountRequest;
import com.bankapp.accounts.api.dto.OpenAccountRequest;
import com.bankapp.accounts.application.closeaccount.CloseAccountCommand;
import com.bankapp.accounts.application.closeaccount.CloseAccountHandler;
import com.bankapp.accounts.application.depositmoney.DepositMoneyCommand;
import com.bankapp.accounts.application.depositmoney.DepositMoneyHandler;
import com.bankapp.accounts.application.freezeaccount.FreezeAccountCommand;
import com.bankapp.accounts.application.freezeaccount.FreezeAccountHandler;
import com.bankapp.accounts.application.getaccount.AccountView;
import com.bankapp.accounts.application.getaccount.GetAccountHandler;
import com.bankapp.accounts.application.openaccount.OpenAccountCommand;
import com.bankapp.accounts.application.openaccount.OpenAccountHandler;
import com.bankapp.accounts.application.openaccount.OpenAccountResult;
import com.bankapp.accounts.application.unfreezeaccount.UnfreezeAccountCommand;
import com.bankapp.accounts.application.unfreezeaccount.UnfreezeAccountHandler;
import com.bankapp.accounts.application.withdrawmoney.WithdrawMoneyCommand;
import com.bankapp.accounts.application.withdrawmoney.WithdrawMoneyHandler;
import com.bankapp.accounts.domain.exceptions.AccountNotFoundException;
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
@RequestMapping("/api/accounts")
public class AccountController {

    private final OpenAccountHandler openAccount;
    private final GetAccountHandler getAccount;
    private final FreezeAccountHandler freezeAccount;
    private final UnfreezeAccountHandler unfreezeAccount;
    private final CloseAccountHandler closeAccount;
    private final DepositMoneyHandler depositMoney;
    private final WithdrawMoneyHandler withdrawMoney;

    public AccountController(
        OpenAccountHandler openAccount,
        GetAccountHandler getAccount,
        FreezeAccountHandler freezeAccount,
        UnfreezeAccountHandler unfreezeAccount,
        CloseAccountHandler closeAccount,
        DepositMoneyHandler depositMoney,
        WithdrawMoneyHandler withdrawMoney
    ) {
        this.openAccount = openAccount;
        this.getAccount = getAccount;
        this.freezeAccount = freezeAccount;
        this.unfreezeAccount = unfreezeAccount;
        this.closeAccount = closeAccount;
        this.depositMoney = depositMoney;
        this.withdrawMoney = withdrawMoney;
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

    /**
     * Throws rather than returning {@code notFound().build()}, so an unknown id
     * answers with a ProblemDetail body like every other error in this API.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> get(@PathVariable UUID id) {
        AccountView view = getAccount
            .handle(id)
            .orElseThrow(() -> new AccountNotFoundException(id));

        return ResponseEntity.ok(AccountResponse.from(view));
    }

    @PostMapping("/{id}/freeze")
    public ResponseEntity<AccountResponse> freeze(@PathVariable UUID id) {
        AccountView view = freezeAccount.handle(new FreezeAccountCommand(id));

        return ResponseEntity.ok(AccountResponse.from(view));
    }

    @PostMapping("/{id}/unfreeze")
    public ResponseEntity<AccountResponse> unfreeze(@PathVariable UUID id) {
        AccountView view = unfreezeAccount.handle(
            new UnfreezeAccountCommand(id)
        );

        return ResponseEntity.ok(AccountResponse.from(view));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<AccountResponse> close(@PathVariable UUID id) {
        AccountView view = closeAccount.handle(
            new CloseAccountCommand(id)
        );

        return ResponseEntity.ok(AccountResponse.from(view));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<AccountResponse> deposit(
        @PathVariable UUID id,
        @Valid @RequestBody AmountRequest request
    ) {
        AccountView view = depositMoney.handle(
            new DepositMoneyCommand(id, request.amount(), request.currencyCode())
        );

        return ResponseEntity.ok(AccountResponse.from(view));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<AccountResponse> withdraw(
        @PathVariable UUID id,
        @Valid @RequestBody AmountRequest request
    ) {
        AccountView view = withdrawMoney.handle(
            new WithdrawMoneyCommand(
                id,
                request.amount(),
                request.currencyCode()
            )
        );

        return ResponseEntity.ok(AccountResponse.from(view));
    }
}
