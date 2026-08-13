package com.bankapp.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankapp.TestcontainersConfiguration;
import com.bankapp.accounts.api.dto.AccountResponse;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
class OpenAccountE2ETest {

    private static final String DEV_USER_ID =
        "11111111-1111-1111-1111-111111111111";

    @Autowired
    private RestTestClient client;

    @Test
    void opensAccountAndReadsItBack() {
        var result = client
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
            .expectHeader()
            .exists("Location")
            .expectBody(AccountResponse.class)
            .returnResult();

        AccountResponse created = result.getResponseBody();
        String location = result.getResponseHeaders().getFirst("Location");

        assertThat(created).isNotNull();
        assertThat(created.status()).isEqualTo("ACTIVE");
        assertThat(created.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(created.accountNumber()).hasSize(10);
        assertThat(location).isEqualTo("/api/accounts/" + created.id());

        client
            .get()
            .uri(location)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AccountResponse.class)
            .isEqualTo(created);
    }

    @Test
    void rejectsRequestWithMissingFields() {
        client
            .post()
            .uri("/api/accounts/")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("type", "CHECKING"))
            .exchange()
            .expectStatus()
            .isBadRequest();
    }

    @Test
    void rejectsUnknownOwner() {
        client
            .post()
            .uri("/api/accounts/")
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                Map.of(
                    "ownerId",
                    UUID.randomUUID().toString(),
                    "type",
                    "CHECKING",
                    "currencyCode",
                    "USD"
                )
            )
            .exchange()
            .expectStatus()
            .isEqualTo(409);
    }

    @Test
    void returns404ForUnknownAccount() {
        client
            .get()
            .uri("/api/accounts/" + UUID.randomUUID())
            .exchange()
            .expectStatus()
            .isNotFound();
    }
}
