package com.bankapp.notifications.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankapp.AbstractE2ETest;
import com.bankapp.accounts.api.dto.AccountResponse;
import com.bankapp.auth.api.dto.AuthResponse;
import com.bankapp.notifications.api.dto.NotificationResponse;
import com.bankapp.notifications.application.port.NotificationChannel;
import com.bankapp.notifications.domain.NotificationStatus;
import com.bankapp.notifications.infrastructure.events.FraudAlertRaisedMessage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.core.ParameterizedTypeReference;

@Import(NotificationsFailureE2ETest.FailingChannelConfiguration.class)
class NotificationsFailureE2ETest extends AbstractE2ETest {

    @TestConfiguration
    static class FailingChannelConfiguration {

        @Bean
        @Primary
        NotificationChannel failingChannel() {
            return notification -> {
                throw new IllegalStateException("channel down");
            };
        }
    }

    @Autowired
    private RestTestClient client;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void failingChannelMarksTheNotificationFailedAndTheListenerCarriesOn()
        throws Exception {
        AuthResponse user = client
            .post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
                "email", "user-" + UUID.randomUUID() + "@example.com",
                "fullName", "Test User",
                "password", "correct horse battery"
            ))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(AuthResponse.class)
            .returnResult()
            .getResponseBody();

        AccountResponse account = client
            .post()
            .uri("/api/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
                "ownerId", user.userId().toString(),
                "type", "CHECKING",
                "currencyCode", "USD"
            ))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();

        sendAlert(account.id());
        sendAlert(account.id());

        List<NotificationResponse> notifications = awaitNotifications(
            user.accessToken(), 2
        );
        assertThat(notifications).hasSize(2);
        assertThat(notifications)
            .allSatisfy(n ->
                assertThat(n.status()).isEqualTo(NotificationStatus.FAILED)
            );
    }

    private void sendAlert(UUID fromAccountId) throws Exception {
        FraudAlertRaisedMessage message = new FraudAlertRaisedMessage(
            UUID.randomUUID(),
            UUID.randomUUID(),
            fromAccountId,
            new BigDecimal("15000"),
            "USD",
            Instant.now()
        );
        kafkaTemplate
            .send("fraud-alert-raised", message.transferId().toString(), message)
            .get(10, TimeUnit.SECONDS);
    }

    private List<NotificationResponse> awaitNotifications(
        String accessToken, int expectedCount
    ) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        List<NotificationResponse> notifications = List.of();
        while (System.currentTimeMillis() < deadline) {
            notifications = client
                .get()
                .uri("/api/notifications")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<List<NotificationResponse>>() {})
                .returnResult()
                .getResponseBody();
            if (notifications.size() == expectedCount) {
                return notifications;
            }
            Thread.sleep(200);
        }
        return notifications;
    }
}
