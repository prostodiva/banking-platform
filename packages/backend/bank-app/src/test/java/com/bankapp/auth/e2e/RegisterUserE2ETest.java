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

class RegisterUserE2ETest extends AbstractE2ETest {

    @Autowired
    private RestTestClient client;

    @Autowired
    private JdbcClient db;

    private static String uniqueEmail() {
        return "ann-" + UUID.randomUUID() + "@example.com";
    }

    private Map<String, String> body(String email, String password) {
        return Map.of("email", email, "fullName", "Ann Lee", "password", password);
    }

    @Test
    void registersAndReturnsBothTokens() {
        String email = uniqueEmail();

        AuthResponse response = client
            .post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body(email, "correct horse battery"))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(AuthResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.expiresAt()).isNotNull();
    }

    @Test
    void issuesACustomerTokenThatCarriesNoPersonalData() {
        AuthResponse response = register(uniqueEmail(), "correct horse battery");

        String payload = new String(
            Base64.getUrlDecoder().decode(response.accessToken().split("\\.")[1]),
            StandardCharsets.UTF_8
        );

        assertThat(payload).contains("\"roles\":[\"CUSTOMER\"]");
        // Anyone holding the token can read this. Nothing in it may identify a
        // human (docs/07): no address, no name.
        assertThat(payload).doesNotContain("@");
        assertThat(payload).doesNotContain("Ann");
    }

    @Test
    void storesOnlyAHashOfTheRefreshToken() {
        AuthResponse response = register(uniqueEmail(), "correct horse battery");

        long matches = db
            .sql("select count(*) from refresh_tokens where token_hash = ?")
            .param(response.refreshToken())
            .query(Long.class)
            .single();

        assertThat(matches).isZero();
    }

    @Test
    void neverPutsCredentialsInTheResponse() {
        String raw = client
            .post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body(uniqueEmail(), "correct horse battery"))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertThat(raw).doesNotContain("passwordHash");
        assertThat(raw).doesNotContain("$2b$");
        assertThat(raw).doesNotContain("correct horse battery");
    }

    @Test
    void rejectsADuplicateEmail() {
        String email = uniqueEmail();
        register(email, "correct horse battery");

        client
            .post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body(email, "a different password"))
            .exchange()
            .expectStatus()
            .isEqualTo(409);
    }

    @Test
    void treatsTheSameAddressInDifferentCaseAsDuplicate() {
        String email = uniqueEmail();
        register(email, "correct horse battery");

        client
            .post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body(email.toUpperCase(), "correct horse battery"))
            .exchange()
            .expectStatus()
            .isEqualTo(409);
    }

    @Test
    void rejectsAShortPassword() {
        client
            .post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body(uniqueEmail(), "short"))
            .exchange()
            .expectStatus()
            .isBadRequest();
    }

    @Test
    void rejectsMissingFields() {
        client
            .post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("email", uniqueEmail()))
            .exchange()
            .expectStatus()
            .isBadRequest()
            // Assert the body, not just the status. Boot's own advice also
            // answers 400 here — with a field-less "Invalid request content." —
            // so a status-only assertion passes whether or not
            // GlobalExceptionHandler was ever reached. That is exactly how this
            // regressed unnoticed from slice 1 until the auth slice.
            .expectBody()
            .jsonPath("$.title")
            .isEqualTo("Validation failed")
            .jsonPath("$.detail")
            .value(detail ->
                assertThat((String) detail).contains("fullName", "password")
            );
    }

    @Test
    void ignoresARoleTheClientTriesToClaim() {
        AuthResponse response = client
            .post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
                "email", uniqueEmail(),
                "fullName", "Mallory",
                "password", "correct horse battery",
                "role", "ADMIN"
            ))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(AuthResponse.class)
            .returnResult()
            .getResponseBody();

        String payload = new String(
            Base64.getUrlDecoder().decode(response.accessToken().split("\\.")[1]),
            StandardCharsets.UTF_8
        );

        assertThat(payload).contains("\"roles\":[\"CUSTOMER\"]");
        assertThat(payload).doesNotContain("ADMIN");
    }

    private AuthResponse register(String email, String password) {
        return client
            .post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body(email, password))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(AuthResponse.class)
            .returnResult()
            .getResponseBody();
    }
}
