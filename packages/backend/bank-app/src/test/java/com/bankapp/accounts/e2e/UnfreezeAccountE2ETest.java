package com.bankapp.accounts.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankapp.TestcontainersConfiguration;
import com.bankapp.accounts.api.dto.AccountResponse;
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
class UnfreezeAccountE2ETest {

    private static final String DEV_USER_ID =
        "11111111-1111-1111-1111-111111111111";

    @Autowired
    private RestTestClient client;

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

    private AccountResponse frozenAccount() {
        AccountResponse account = openAccount();

        return client
            .post()
            .uri("/api/accounts/" + account.id() + "/freeze")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();
    }

    @Test
    void unfreezeFrozenAccount() {
        //arrange a frozen account
        AccountResponse frozen = frozenAccount();

        //act + assert
        AccountResponse unfrozen = client
            .post()
            .uri("/api/accounts/" + frozen.id() + "/unfreeze")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(unfrozen).isNotNull();
        assertThat(unfrozen.status()).isEqualTo("ACTIVE");
    }

    @Test
    void returns404ForUnknownAccount() {
        client
            .post()
            .uri("/api/accounts/" + UUID.randomUUID() + "/unfreeze")
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void returns409WhenAccountIsNotFrozen() {
        // arrange — an ACTIVE account
        AccountResponse account = openAccount();

        // act + assert
        client
            .post()
            .uri("/api/accounts/" + account.id() + "/unfreeze")
            .exchange()
            .expectStatus()
            .isEqualTo(409);
    }
}
