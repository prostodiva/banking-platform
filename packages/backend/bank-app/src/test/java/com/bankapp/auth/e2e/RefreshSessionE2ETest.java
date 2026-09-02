package com.bankapp.auth.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankapp.AbstractE2ETest;
import com.bankapp.auth.api.dto.AuthResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.RestTestClient;

class RefreshSessionE2ETest extends AbstractE2ETest {

    private static final String PASSWORD = "correct horse battery";

    @Autowired
    private RestTestClient client;

    @Autowired
    private JdbcClient db;

    private record Session(String email, AuthResponse tokens) {}

    private Session register() {
        String email = "cara-" + UUID.randomUUID() + "@example.com";
        AuthResponse tokens = client
            .post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("email", email, "fullName", "Cara Diaz", "password", PASSWORD))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(AuthResponse.class)
            .returnResult()
            .getResponseBody();
        return new Session(email, tokens);
    }

    private AuthResponse refresh(String token) {
        return client
            .post()
            .uri("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("refreshToken", token))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AuthResponse.class)
            .returnResult()
            .getResponseBody();
    }

    private String refreshFailure(String token) {
        return client
            .post()
            .uri("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("refreshToken", token))
            .exchange()
            .expectStatus()
            .isEqualTo(401)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
    }

    private long activeTokensFor(String email) {
        return db
            .sql("""
                select count(*) from refresh_tokens t
                join users u on u.id = t.user_id
                where u.email = ? and t.revoked_at is null
                """)
            .param(email)
            .query(Long.class)
            .single();
    }

    @Test
    void rotatesToANewPair() {
        Session session = register();

        AuthResponse rotated = refresh(session.tokens().refreshToken());

        assertThat(rotated.refreshToken()).isNotEqualTo(session.tokens().refreshToken());
        assertThat(rotated.accessToken()).isNotBlank();
        assertThat(activeTokensFor(session.email())).isEqualTo(1);
    }

    @Test
    void thePresentedTokenIsSingleUse() {
        Session session = register();
        refresh(session.tokens().refreshToken());

        refreshFailure(session.tokens().refreshToken());
    }

    /** The reason this story exists. */
    @Test
    void reusingASpentTokenEndsEverySession() {
        Session session = register();

        // a second device
        AuthResponse otherDevice = client
            .post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("email", session.email(), "password", PASSWORD))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AuthResponse.class)
            .returnResult()
            .getResponseBody();

        AuthResponse rotated = refresh(session.tokens().refreshToken());
        assertThat(activeTokensFor(session.email())).isEqualTo(2);

        // the spent token comes back — assume theft
        refreshFailure(session.tokens().refreshToken());

        assertThat(activeTokensFor(session.email())).isZero();
        refreshFailure(rotated.refreshToken());
        refreshFailure(otherDevice.refreshToken());
    }

    @Test
    void unknownAndReusedTokensAreIndistinguishable() {
        Session session = register();
        refresh(session.tokens().refreshToken());

        String reused = refreshFailure(session.tokens().refreshToken());
        String unknown = refreshFailure(RandomToken.value());

        assertThat(reused).isEqualTo(unknown);
    }

    @Test
    void rejectsAMissingToken() {
        client
            .post()
            .uri("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of())
            .exchange()
            .expectStatus()
            .isBadRequest();
    }

    private static final class RandomToken {
        static String value() {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
