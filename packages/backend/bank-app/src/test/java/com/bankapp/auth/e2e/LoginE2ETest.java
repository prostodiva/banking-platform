package com.bankapp.auth.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankapp.AbstractE2ETest;
import com.bankapp.auth.api.dto.AuthResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.RestTestClient;

class LoginE2ETest extends AbstractE2ETest {

    private static final String PASSWORD = "correct horse battery";

    @Autowired
    private RestTestClient client;

    @Autowired
    private JdbcClient db;

    private String registeredEmail() {
        String email = "bea-" + UUID.randomUUID() + "@example.com";
        client
            .post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("email", email, "fullName", "Bea Vance", "password", PASSWORD))
            .exchange()
            .expectStatus()
            .isCreated();
        return email;
    }

    private String loginBody(String email, String password, int expectedStatus) {
        return client
            .post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("email", email, "password", password))
            .exchange()
            .expectStatus()
            .isEqualTo(expectedStatus)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
    }

    @Test
    void logsInAndReturnsBothTokens() {
        String email = registeredEmail();

        AuthResponse response = client
            .post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("email", email, "password", PASSWORD))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AuthResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void findsTheAccountRegardlessOfHowTheAddressIsCased() {
        String email = registeredEmail();

        client
            .post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("email", email.toUpperCase(), "password", PASSWORD))
            .exchange()
            .expectStatus()
            .isOk();
    }

    /** The property this story exists for. */
    @Test
    void wrongPasswordAndUnknownEmailAreIndistinguishable() {
        String email = registeredEmail();

        String wrongPassword = loginBody(email, "not the password", 401);
        String unknownEmail = loginBody("nobody-" + UUID.randomUUID() + "@example.com", PASSWORD, 401);

        assertThat(wrongPassword).isEqualTo(unknownEmail);
        assertThat(wrongPassword).doesNotContain("not found", "no such", "exist");
    }

    @Test
    void issuesACustomerTokenCarryingNoPersonalData() {
        String email = registeredEmail();

        AuthResponse response = client
            .post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("email", email, "password", PASSWORD))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AuthResponse.class)
            .returnResult()
            .getResponseBody();

        String payload = new String(
            Base64.getUrlDecoder().decode(response.accessToken().split("\\.")[1]),
            StandardCharsets.UTF_8
        );

        assertThat(payload).contains("\"roles\":[\"CUSTOMER\"]");
        assertThat(payload).doesNotContain("@");
    }

    @Test
    void leavesExistingSessionsAlone() {
        String email = registeredEmail();

        client
            .post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("email", email, "password", PASSWORD))
            .exchange()
            .expectStatus()
            .isOk();

        long active = db
            .sql("""
                select count(*) from refresh_tokens t
                join users u on u.id = t.user_id
                where u.email = ? and t.revoked_at is null
                """)
            .param(email)
            .query(Long.class)
            .single();

        // One from register, one from login. Signing in on a phone must not sign
        // you out on a laptop.
        assertThat(active).isEqualTo(2);
    }

    @Test
    void rejectsMissingFields() {
        client
            .post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("email", "bea@example.com"))
            .exchange()
            .expectStatus()
            .isBadRequest();
    }

    /** V5 gave the seeded dev user a hash that matches nothing, on purpose. */
    @Test
    void theSeededDevUserCannotLogIn() {
        loginBody("dev@bankapp.local", "password", 401);
    }
}
