package com.bankapp.auth.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankapp.AbstractE2ETest;
import com.bankapp.auth.api.dto.AuthResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class LogoutE2ETest extends AbstractE2ETest {

    private static final String PASSWORD = "correct horse battery";

    @Autowired
    private RestTestClient client;

    private AuthResponse register() {
        return client
            .post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
                "email", "dana-" + UUID.randomUUID() + "@example.com",
                "fullName", "Dana Ruiz",
                "password", PASSWORD
            ))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(AuthResponse.class)
            .returnResult()
            .getResponseBody();
    }

    private void logout(AuthResponse session, String refreshToken, int expectedStatus) {
        client
            .post()
            .uri("/api/auth/logout")
            .header("Authorization", "Bearer " + session.accessToken())
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("refreshToken", refreshToken))
            .exchange()
            .expectStatus()
            .isEqualTo(expectedStatus);
    }

    private void expectRefreshStatus(String refreshToken, int expectedStatus) {
        client
            .post()
            .uri("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("refreshToken", refreshToken))
            .exchange()
            .expectStatus()
            .isEqualTo(expectedStatus);
    }

    @Test
    void logsOutAndInvalidatesThatRefreshToken() {
        AuthResponse session = register();

        logout(session, session.refreshToken(), 204);

        expectRefreshStatus(session.refreshToken(), 401);
    }

    @Test
    void isIdempotent() {
        AuthResponse session = register();

        logout(session, session.refreshToken(), 204);
        logout(session, session.refreshToken(), 204);
    }

    @Test
    void ignoresAnUnknownRefreshToken() {
        AuthResponse session = register();

        logout(session, UUID.randomUUID().toString(), 204);

        // and the caller's real session is untouched
        expectRefreshStatus(session.refreshToken(), 200);
    }

    /** Someone else's token is not an error, and not revoked either. */
    @Test
    void ignoresARefreshTokenBelongingToAnotherUser() {
        AuthResponse mallory = register();
        AuthResponse victim = register();

        logout(mallory, victim.refreshToken(), 204);

        expectRefreshStatus(victim.refreshToken(), 200);
    }

    @Test
    void leavesOtherDevicesSignedIn() {
        AuthResponse first = register();
        AuthResponse second = client
            .post()
            .uri("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("refreshToken", first.refreshToken()))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AuthResponse.class)
            .returnResult()
            .getResponseBody();

        // logging out with a token that is already spent must not touch the live one
        logout(second, first.refreshToken(), 204);

        expectRefreshStatus(second.refreshToken(), 200);
    }

    /** The matcher-order test. Without it, "silently public" ships. */
    @Test
    void requiresAnAccessToken() {
        AuthResponse session = register();

        String body = client
            .post()
            .uri("/api/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("refreshToken", session.refreshToken()))
            .exchange()
            .expectStatus()
            .isUnauthorized()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertThat(body).contains("\"status\":401");
        assertThat(body).contains("Invalid credentials");
    }

    /**
     * The real matcher-order test, and the reason {@link #requiresAnAccessToken}
     * is not one: no token AND an invalid body. Authentication runs in the filter
     * chain, before argument resolution, so the answer must be 401 — the body is
     * never looked at.
     *
     * <p>Move {@code .requestMatchers(POST, "/api/auth/logout").authenticated()}
     * below the {@code /api/auth/**} wildcard and this returns 400 instead: the
     * endpoint is public, {@code @Valid} rejects the empty body, and the caller is
     * told what is wrong with a request they were never entitled to make.
     */
    @Test
    void authenticatesBeforeItValidates() {
        client
            .post()
            .uri("/api/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of())
            .exchange()
            .expectStatus()
            .isUnauthorized();
    }

    @Test
    void rejectsAMalformedAccessToken() {
        client
            .post()
            .uri("/api/auth/logout")
            .header("Authorization", "Bearer not.a.token")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("refreshToken", "whatever"))
            .exchange()
            .expectStatus()
            .isUnauthorized();
    }

    @Test
    void rejectsAMissingRefreshToken() {
        AuthResponse session = register();

        client
            .post()
            .uri("/api/auth/logout")
            .header("Authorization", "Bearer " + session.accessToken())
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of())
            .exchange()
            .expectStatus()
            .isBadRequest();
    }
}
