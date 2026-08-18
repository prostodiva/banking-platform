package com.bankapp.accounts.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.bankapp.TestcontainersConfiguration;
import com.bankapp.accounts.api.dto.AccountResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
public class WithdrawMoneyE2ETest {

    @Autowired
    private RestTestClient client;

    private static final String DEV_USER_ID =
        "11111111-1111-1111-1111-111111111111";

    private AccountResponse openAccount() {
        return client
            .post()
            .uri("/api/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                Map.of(
                    "ownerId",
                    DEV_USER_ID,
                    "type",
                    "CHECKING",
                    "currencyCode",
                    "USD"
                )
            )
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();
    }

    private AccountResponse fundedAccount(String amount) {
        AccountResponse account = openAccount();

        return client
            .post()
            .uri("/api/accounts/" + account.id() + "/deposit")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("amount", new BigDecimal(amount), "currencyCode", "USD"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();
    }

    private RestTestClient.ResponseSpec withdraw(
        UUID accountId,
        String amount,
        String currencyCode
    ) {
        return client
            .post()
            .uri("/api/accounts/" + accountId + "/withdraw")
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                Map.of(
                    "amount",
                    new BigDecimal(amount),
                    "currencyCode",
                    currencyCode
                )
            )
            .exchange();
    }

    @Test
    void returns409WhenBalanceIsInsufficient() {
        // arrange — an account holding 50.00
        AccountResponse account = fundedAccount("50.00");

        // act + assert — asking for 100.00 is refused
        withdraw(account.id(), "100.00", "USD").expectStatus().isEqualTo(409);

        // and the money is still there
        AccountResponse after = client
            .get()
            .uri("/api/accounts/" + account.id())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(after).isNotNull();
        assertThat(after.balance()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void returns200WhenBalanceIsEnough() {
        AccountResponse account = fundedAccount("50.00");

        withdraw(account.id(), "20.00", "USD").expectStatus().isOk();

        AccountResponse after = client
            .get()
            .uri("/api/accounts/" + account.id())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(after).isNotNull();
        assertThat(after.balance()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    void returns409WhenWithdrawFromClosed() {
        // arrange — a CLOSED account (close requires a zero balance,
        // so this one is never funded)
        AccountResponse account = openAccount();
        client
            .post()
            .uri("/api/accounts/" + account.id() + "/close")
            .exchange()
            .expectStatus()
            .isOk();

        // act + assert — status is checked before funds, so this is
        // 409 for being CLOSED, not for being empty
        withdraw(account.id(), "20.00", "USD").expectStatus().isEqualTo(409);
    }

    @Test
    void returns409WhenWithdrawFromFrozen() {
        AccountResponse account = openAccount();
        client
            .post()
            .uri("/api/accounts/" + account.id() + "/freeze")
            .exchange()
            .expectStatus()
            .isOk();

        withdraw(account.id(), "20.00", "USD").expectStatus().isEqualTo(409);
    }

    @Test
    void returns400WhenWithdrawalAmountIsNonPositive() {
        AccountResponse account = fundedAccount("50.00");

        withdraw(account.id(), "-20.00", "USD").expectStatus().isBadRequest();
        withdraw(account.id(), "0.00", "USD").expectStatus().isBadRequest();
    }

    @Test
    void returns400WhenWithdrawalCurrencyDoesntMatch() {
        AccountResponse account = fundedAccount("50.00");

        withdraw(account.id(), "20.00", "RUB").expectStatus().isBadRequest();
    }

    @Test
    void returns404ForUnknownAccount() {
        withdraw(UUID.randomUUID(), "20.00", "USD").expectStatus().isNotFound();
    }
}
