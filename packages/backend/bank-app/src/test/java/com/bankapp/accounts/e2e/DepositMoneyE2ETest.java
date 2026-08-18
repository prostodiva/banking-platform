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
public class DepositMoneyE2ETest {

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

        private RestTestClient.ResponseSpec deposit(
                UUID accountId,
                String amount,
                String currencyCode
            ) {
                return client
                    .post()
                    .uri("/api/accounts/" + accountId + "/deposit")
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
    void depositsIntoActiveAccount() {
        // arrange — an ACTIVE account with a zero balance
        AccountResponse created = openAccount();

        // act + assert
        AccountResponse deposited = deposit(created.id(), "100.00", "USD")
            .expectStatus()
            .isOk()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(deposited).isNotNull();
        assertThat(deposited.balance()).isEqualByComparingTo(
            new BigDecimal("100.00")
        );
        assertThat(deposited.status()).isEqualTo("ACTIVE");
    }

    @Test
    void depositAccumulatesBalance() {
        AccountResponse created = openAccount();

        deposit(created.id(), "100.00", "USD").expectStatus().isOk();

        AccountResponse deposited = deposit(created.id(), "25.50", "USD")
            .expectStatus()
            .isOk()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(deposited).isNotNull();
        assertThat(deposited.balance()).isEqualByComparingTo(
            new BigDecimal("125.50")
        );
    }

    @Test
    void returns409WhenAccountIsFrozen() {
        // arrange — a FROZEN account
        AccountResponse created = openAccount();
        client
            .post()
            .uri("/api/accounts/" + created.id() + "/freeze")
            .exchange()
            .expectStatus()
            .isOk();

        // act + assert — money does not move on a frozen account
        deposit(created.id(), "100.00", "USD").expectStatus().isEqualTo(409);

        // and the balance is untouched
        AccountResponse after = client
            .get()
            .uri("/api/accounts/" + created.id())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(after).isNotNull();
        assertThat(after.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void returns409WhenAccountIsClosed() {
        AccountResponse created = openAccount();
        client
            .post()
            .uri("/api/accounts/" + created.id() + "/close")
            .exchange()
            .expectStatus()
            .isOk();

        deposit(created.id(), "100.00", "USD").expectStatus().isEqualTo(409);
    }

    @Test
    void returns400ForNonPositiveAmount() {
        AccountResponse created = openAccount();

        deposit(created.id(), "-50.00", "USD").expectStatus().isBadRequest();
        deposit(created.id(), "0.00", "USD").expectStatus().isBadRequest();
    }

    @Test
    void returns400ForMismatchedCurrency() {
        AccountResponse created = openAccount();

        deposit(created.id(), "50.00", "RUB").expectStatus().isBadRequest();
    }

    @Test
    void returns404ForUnknownAccount() {
        deposit(UUID.randomUUID(), "50.00", "USD").expectStatus().isNotFound();
    }
}
