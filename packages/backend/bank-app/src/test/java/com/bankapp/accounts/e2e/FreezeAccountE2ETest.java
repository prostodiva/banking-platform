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
class FreezeAccountE2ETest {

    private static final String DEV_USER_ID =
        "11111111-1111-1111-1111-111111111111";

    @Autowired
    private RestTestClient client;

    private AccountResponse openAccount() {
        return client
            .post()
            .uri("/api/accounts/")
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
    void freezesActiveAccount() {
        // arrange — an ACTIVE account
        AccountResponse created = openAccount();

        // act + assert
        AccountResponse frozen = client
            .post()
            .uri("/api/accounts/" + created.id() + "/freeze")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(frozen).isNotNull();
        assertThat(frozen.status()).isEqualTo("FROZEN");
    }

    @Test
    void returns404ForUnknownAccount() {
        client
            .post()
            .uri("/api/accounts/" + UUID.randomUUID() + "/freeze")
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void returns409WhenAccountIsAlreadyFrozen() {
        // arrange — an account that is already FROZEN
        AccountResponse account = openAccount();
        client
            .post()
            .uri("/api/accounts/" + account.id() + "/freeze")
            .exchange()
            .expectStatus()
            .isOk();

        // act + assert — a second freeze is refused
        client
            .post()
            .uri("/api/accounts/" + account.id() + "/freeze")
            .exchange()
            .expectStatus()
            .isEqualTo(409);
    }
}
