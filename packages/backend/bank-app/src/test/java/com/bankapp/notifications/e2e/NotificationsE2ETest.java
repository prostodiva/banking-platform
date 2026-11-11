package com.bankapp.notifications.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankapp.AbstractE2ETest;
import com.bankapp.accounts.api.dto.AccountResponse;
import com.bankapp.auth.api.dto.AuthResponse;
import com.bankapp.notifications.api.dto.NotificationResponse;
import com.bankapp.notifications.domain.NotificationStatus;
import com.bankapp.notifications.domain.NotificationType;
import com.bankapp.notifications.infrastructure.events.FraudAlertRaisedMessage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.core.ParameterizedTypeReference;


@TestPropertySource(properties = "fraud.large-payment-threshold=10000")
class NotificationsE2ETest extends AbstractE2ETest {

    private static final String PASSWORD = "correct horse battery";

    @Autowired
    private RestTestClient client;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private AuthResponse register() {
        return client
            .post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
                "email", "user-" + UUID.randomUUID() + "@example.com",
                "fullName", "Test User",
                "password", PASSWORD
            ))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(AuthResponse.class)
            .returnResult()
            .getResponseBody();
    }

    private AccountResponse openAccount(UUID ownerId) {
        return client
            .post()
            .uri("/api/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
                "ownerId", ownerId.toString(),
                "type", "CHECKING",
                "currencyCode", "USD"
            ))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();
    }

    private AccountResponse fundedAccount(UUID ownerId, String amount) {
        AccountResponse account = openAccount(ownerId);
        client
            .post()
            .uri("/api/accounts/" + account.id() + "/deposit")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("amount", new BigDecimal(amount), "currencyCode", "USD"))
            .exchange()
            .expectStatus()
            .isOk();
        return account;
    }


    private void transfer(UUID from, UUID to, String amount) {
        Map<String, Object> body = new HashMap<>();
        body.put("fromAccountId", from);
        body.put("toAccountId", to);
        body.put("amount", new BigDecimal(amount));
        body.put("currencyCode", "USD");

        client
            .post()
            .uri("/api/payments/transfers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body(body)
            .exchange()
            .expectStatus()
            .isCreated();
    }

    private List<NotificationResponse> notificationsFor(String accessToken) {
        return client
            .get()
            .uri("/api/notifications")
            .header("Authorization", "Bearer " + accessToken)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new ParameterizedTypeReference<List<NotificationResponse>>() {})
            .returnResult()
            .getResponseBody();
    }

    private List<NotificationResponse> awaitNotifications(
        String accessToken, int expectedCount
    ) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        List<NotificationResponse> notifications = List.of();
        while (System.currentTimeMillis() < deadline) {
            notifications = notificationsFor(accessToken);
            if (notifications.size() == expectedCount) {
                return notifications;
            }
            Thread.sleep(200);
        }
        return notifications;
    }

    private void sendAlert(UUID alertId, UUID fromAccountId, String amount) throws Exception {
        FraudAlertRaisedMessage message = new FraudAlertRaisedMessage(
            alertId,
            UUID.randomUUID(),
            fromAccountId,
            new BigDecimal(amount),
            "USD",
            Instant.now()
        );
        kafkaTemplate
            .send("fraud-alert-raised", message.transferId().toString(), message)
            .get(10, TimeUnit.SECONDS);
    }

    @Test
    void largeTransferCreatesANotificationForTheSender() throws Exception {
        AuthResponse sender = register();
        AuthResponse receiver = register();
        AccountResponse from = fundedAccount(sender.userId(), "20000");
        AccountResponse to = openAccount(receiver.userId());

        transfer(from.id(), to.id(), "15000");

        List<NotificationResponse> notifications = awaitNotifications(
            sender.accessToken(), 1
        );
        assertThat(notifications).hasSize(1);
        NotificationResponse notification = notifications.get(0);
        assertThat(notification.type()).isEqualTo(NotificationType.FRAUD_ALERT);
        assertThat(notification.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.accountId()).isEqualTo(from.id());
        assertThat(notification.amount()).isEqualByComparingTo(new BigDecimal("15000"));
    }

    @Test
    void duplicateEventDeliveryCreatesExactlyOneNotification() throws Exception {
        AuthResponse user = register();
        AccountResponse account = fundedAccount(user.userId(), "20000");
        UUID alertId = UUID.randomUUID();

        sendAlert(alertId, account.id(), "15000");
        sendAlert(alertId, account.id(), "15000");

        assertThat(awaitNotifications(user.accessToken(), 1)).hasSize(1);
        Thread.sleep(2000);
        assertThat(notificationsFor(user.accessToken())).hasSize(1);
    }

    @Test
    void malformedEventIsSkippedAndLaterEventsStillProcessed() throws Exception {
        AuthResponse user = register();
        AccountResponse account = fundedAccount(user.userId(), "20000");

        sendAlert(null, account.id(), "15000");
        sendAlert(UUID.randomUUID(), account.id(), "15000");

        assertThat(awaitNotifications(user.accessToken(), 1)).hasSize(1);
    }

    @Test
    void usersSeeOnlyTheirOwnNotifications() throws Exception {
        AuthResponse owner = register();
        AuthResponse other = register();
        AccountResponse account = fundedAccount(owner.userId(), "20000");

        sendAlert(UUID.randomUUID(), account.id(), "15000");

        assertThat(awaitNotifications(owner.accessToken(), 1)).hasSize(1);
        assertThat(notificationsFor(other.accessToken())).isEmpty();
    }

    @Test
    void listingRequiresAuthentication() {
        client
            .get()
            .uri("/api/notifications")
            .exchange()
            .expectStatus()
            .isUnauthorized();
    }
}
