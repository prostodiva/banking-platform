package com.bankapp.accounts.e2e;

import static org.assertj.core.api.Assertions.assertThat;

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
public class CloseAccountE2ETest {

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

    @Test
    void closesActiveAccount() {
        // arrange — an ACTIVE account
        AccountResponse created = openAccount();

        // act + assert
        AccountResponse closed = client
            .post()
            .uri("/api/accounts/" + created.id() + "/close")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(closed).isNotNull();
        assertThat(closed.status()).isEqualTo("CLOSED");
    }

    @Test
    void closesFrozenAccount() {
        // arrange — a FROZEN account
        AccountResponse created = openAccount();
        client
            .post()
            .uri("/api/accounts/" + created.id() + "/freeze")
            .exchange()
            .expectStatus()
            .isOk();

        // act + assert — FROZEN is a legal source state for close
        AccountResponse closed = client
            .post()
            .uri("/api/accounts/" + created.id() + "/close")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(closed).isNotNull();
        assertThat(closed.status()).isEqualTo("CLOSED");
    }

    @Test
    void returns404ForUnknownAccount() {
        client
            .post()
            .uri("/api/accounts/" + UUID.randomUUID() + "/close")
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void returns409WhenAccountIsAlreadyClosed() {
        // arrange — an account that is already CLOSED
        AccountResponse account = openAccount();
        client
            .post()
            .uri("/api/accounts/" + account.id() + "/close")
            .exchange()
            .expectStatus()
            .isOk();

        // act + assert — a second close is refused
        client
            .post()
            .uri("/api/accounts/" + account.id() + "/close")
            .exchange()
            .expectStatus()
            .isEqualTo(409);
    }
}
