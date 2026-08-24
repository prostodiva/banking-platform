package com.bankapp.payments.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankapp.TestcontainersConfiguration;
import com.bankapp.accounts.api.dto.AccountResponse;
import com.bankapp.payments.api.dto.TransferResponse;
import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
public class TransferMoneyE2ETest {

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

    /** An ACTIVE account holding {@code amount} USD. */
    private AccountResponse fundedAccount(String amount) {
        AccountResponse account = openAccount();
        client
            .post()
            .uri("/api/accounts/" + account.id() + "/deposit")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("amount", new BigDecimal(amount), "currencyCode", "USD"))
            .exchange()
            .expectStatus()
            .isOk();
        return account;
    }

    private RestTestClient.ResponseSpec transfer(
        UUID from,
        UUID to,
        String amount,
        String currencyCode,
        String idempotencyKey
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("fromAccountId", from);
        body.put("toAccountId", to);
        body.put("amount", new BigDecimal(amount));
        body.put("currencyCode", currencyCode);

        RestTestClient.RequestBodySpec request = client
            .post()
            .uri("/api/payments/transfers")
            .contentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) {
            request = request.header("Idempotency-Key", idempotencyKey);
        }
        return request.body(body).exchange();
    }

    private BigDecimal balanceOf(UUID accountId) {
        AccountResponse account = client
            .get()
            .uri("/api/accounts/" + accountId)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(account).isNotNull();
        return account.balance();
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }

    @Test
    void transfersMoneyBetweenTwoAccounts() {
        AccountResponse from = fundedAccount("100.00");
        AccountResponse to = openAccount();

        TransferResponse transfer = transfer(
            from.id(),
            to.id(),
            "25.00",
            "USD",
            newKey()
        )
            .expectStatus()
            .isCreated()
            .expectBody(TransferResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(transfer).isNotNull();
        assertThat(transfer.id()).isNotNull();
        assertThat(transfer.fromAccountId()).isEqualTo(from.id());
        assertThat(transfer.toAccountId()).isEqualTo(to.id());
        assertThat(transfer.amount()).isEqualByComparingTo("25.00");
        assertThat(transfer.createdAt()).isNotNull();

        assertThat(balanceOf(from.id())).isEqualByComparingTo("75.00");
        assertThat(balanceOf(to.id())).isEqualByComparingTo("25.00");
    }

    /** A Location that 404s would be worse than none at all (ADR-003 consequences). */
    @Test
    void locationHeaderResolvesToTheTransfer() {
        AccountResponse from = fundedAccount("100.00");
        AccountResponse to = openAccount();

        URI location = transfer(from.id(), to.id(), "25.00", "USD", newKey())
            .expectStatus()
            .isCreated()
            .expectBody(TransferResponse.class)
            .returnResult()
            .getResponseHeaders()
            .getLocation();

        assertThat(location).isNotNull();

        TransferResponse fetched = client
            .get()
            .uri(location.toString())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(TransferResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(fetched).isNotNull();
        assertThat(fetched.fromAccountId()).isEqualTo(from.id());
        assertThat(fetched.amount()).isEqualByComparingTo("25.00");
    }

    /** The point of idempotency: a blind retry cannot tell it already happened. */
    @Test
    void replayReturnsTheOriginalResponseAndMovesMoneyOnce() {
        AccountResponse from = fundedAccount("100.00");
        AccountResponse to = openAccount();
        String key = newKey();

        TransferResponse first = transfer(
            from.id(),
            to.id(),
            "25.00",
            "USD",
            key
        )
            .expectStatus()
            .isCreated()
            .expectBody(TransferResponse.class)
            .returnResult()
            .getResponseBody();

        TransferResponse replay = transfer(
            from.id(),
            to.id(),
            "25.00",
            "USD",
            key
        )
            .expectStatus()
            .isCreated()
            .expectBody(TransferResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(replay).isNotNull();
        assertThat(first).isNotNull();
        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(replay.createdAt()).isEqualTo(first.createdAt());

        assertThat(balanceOf(from.id())).isEqualByComparingTo("75.00");
        assertThat(balanceOf(to.id())).isEqualByComparingTo("25.00");
    }

    @Test
    void returns422ForSameKeyWithADifferentBody() {
        AccountResponse from = fundedAccount("100.00");
        AccountResponse to = openAccount();
        String key = newKey();

        transfer(from.id(), to.id(), "25.00", "USD", key)
            .expectStatus()
            .isCreated();

        transfer(from.id(), to.id(), "30.00", "USD", key)
            .expectStatus()
            .isEqualTo(422);

        assertThat(balanceOf(from.id())).isEqualByComparingTo("75.00");
    }

    @Test
    void returns400WhenIdempotencyKeyIsMissing() {
        AccountResponse from = fundedAccount("100.00");
        AccountResponse to = openAccount();

        transfer(from.id(), to.id(), "25.00", "USD", null)
            .expectStatus()
            .isBadRequest();

        assertThat(balanceOf(from.id())).isEqualByComparingTo("100.00");
    }

    @Test
    void returns400ForATransferToTheSameAccount() {
        AccountResponse account = fundedAccount("100.00");

        transfer(account.id(), account.id(), "25.00", "USD", newKey())
            .expectStatus()
            .isBadRequest();

        assertThat(balanceOf(account.id())).isEqualByComparingTo("100.00");
    }

    @Test
    void returns400ForANonPositiveAmount() {
        AccountResponse from = fundedAccount("100.00");
        AccountResponse to = openAccount();

        transfer(from.id(), to.id(), "0.00", "USD", newKey())
            .expectStatus()
            .isBadRequest();
        transfer(from.id(), to.id(), "-5.00", "USD", newKey())
            .expectStatus()
            .isBadRequest();
    }

    @Test
    void returns400ForAMismatchedCurrency() {
        AccountResponse from = fundedAccount("100.00");
        AccountResponse to = openAccount();

        transfer(from.id(), to.id(), "25.00", "EUR", newKey())
            .expectStatus()
            .isBadRequest();

        assertThat(balanceOf(from.id())).isEqualByComparingTo("100.00");
    }

    @Test
    void returns409ForInsufficientFunds() {
        AccountResponse from = fundedAccount("10.00");
        AccountResponse to = openAccount();

        transfer(from.id(), to.id(), "25.00", "USD", newKey())
            .expectStatus()
            .isEqualTo(409);

        assertThat(balanceOf(from.id())).isEqualByComparingTo("10.00");
        assertThat(balanceOf(to.id())).isEqualByComparingTo("0.00");
    }

    @Test
    void returns409WhenAnAccountIsNotActive() {
        AccountResponse from = fundedAccount("100.00");
        AccountResponse to = openAccount();

        client
            .post()
            .uri("/api/accounts/" + to.id() + "/freeze")
            .exchange()
            .expectStatus()
            .isOk();

        transfer(from.id(), to.id(), "25.00", "USD", newKey())
            .expectStatus()
            .isEqualTo(409);

        // the debit rolled back with the credit
        assertThat(balanceOf(from.id())).isEqualByComparingTo("100.00");
    }

    @Test
    void returns404ForAnUnknownAccount() {
        AccountResponse from = fundedAccount("100.00");

        transfer(from.id(), UUID.randomUUID(), "25.00", "USD", newKey())
            .expectStatus()
            .isNotFound();

        assertThat(balanceOf(from.id())).isEqualByComparingTo("100.00");
    }

    /** 404 carries a ProblemDetail body, like every other error in this API. */
    @Test
    void returns404ForAnUnknownTransfer() {
        UUID unknown = UUID.randomUUID();

        client
            .get()
            .uri("/api/payments/transfers/" + unknown)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.status")
            .isEqualTo(404)
            .jsonPath("$.detail")
            .value(detail -> assertThat((String) detail).contains(unknown.toString()));
    }
}
