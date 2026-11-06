package com.bankapp.frauddetection.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankapp.AbstractE2ETest;
import com.bankapp.accounts.api.dto.AccountResponse;
import com.bankapp.frauddetection.api.dto.FraudAlertResponse;
import com.bankapp.frauddetection.infrastructure.events.PaymentCompletedMessage;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import com.bankapp.payments.api.dto.TransferResponse;
import org.springframework.core.ParameterizedTypeReference;

@TestPropertySource(properties = "fraud.large-payment-threshold=10000")
class FraudDetectionE2ETest extends AbstractE2ETest {

    private static final String DEV_USER_ID =
        "11111111-1111-1111-1111-111111111111";

    @Autowired
    private RestTestClient client;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void largeTransferRaisesAlert() {
        AccountResponse from = openAccountWithFunds("20000");
        AccountResponse to = openAccount();

        UUID transferId = transfer(from.id(), to.id(), "15000");

        List<FraudAlertResponse> alerts = awaitAlerts(from.id());
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).transferId()).isEqualTo(transferId);
        assertThat(alerts.get(0).amount()).isEqualByComparingTo("15000");
        assertThat(alerts.get(0).status()).isEqualTo("RAISED");
    }

    @Test
    void smallTransferRaisesNoAlert() throws Exception {
        AccountResponse from = openAccountWithFunds("20000");
        AccountResponse to = openAccount();

        transfer(from.id(), to.id(), "100");

        Thread.sleep(2000);
        assertThat(alertsFor(from.id())).isEmpty();
    }

    @Test
    void duplicateEventDeliveryCreatesExactlyOneAlert() {
        UUID transferId = UUID.randomUUID();
        UUID fromAccount = UUID.randomUUID();
        PaymentCompletedMessage message = new PaymentCompletedMessage(
            transferId, fromAccount, UUID.randomUUID(),
            new BigDecimal("15000"), "USD", "dup-test-key", Instant.now()
        );

        kafkaTemplate.send("payment-completed", transferId.toString(), message);
        kafkaTemplate.send("payment-completed", transferId.toString(), message);

        List<FraudAlertResponse> alerts = awaitAlerts(fromAccount);
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).transferId()).isEqualTo(transferId);
    }

    @Test
    void failedTransferRaisesNoAlert() throws Exception {
        AccountResponse from = openAccountWithFunds("100");
        AccountResponse to = openAccount();

        client.post()
            .uri("/api/payments/transfers")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
                "fromAccountId", from.id().toString(),
                "toAccountId", to.id().toString(),
                "amount", new BigDecimal("15000"),
                "currencyCode", "USD"
            ))
            .exchange()
            .expectStatus().isEqualTo(409);


        Thread.sleep(2000);
        assertThat(alertsFor(from.id())).isEmpty();
    }

    private AccountResponse openAccount() {
        return client
            .post()
            .uri("/api/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
                "ownerId", DEV_USER_ID,
                "type", "CHECKING",
                "currencyCode", "USD"
            ))
            .exchange()
            .expectStatus().isCreated()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();
    }

    private AccountResponse openAccountWithFunds(String amount) {
        AccountResponse account = openAccount();
        return client
            .post()
            .uri("/api/accounts/{id}/deposit", account.id())
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
                "amount", new BigDecimal(amount),
                "currencyCode", "USD"
            ))
            .exchange()
            .expectStatus().isOk()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();
    }

    private UUID transfer(UUID fromAccountId, UUID toAccountId, String amount) {
        TransferResponse response = client
            .post()
            .uri("/api/payments/transfers")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
                "fromAccountId", fromAccountId.toString(),
                "toAccountId", toAccountId.toString(),
                "amount", new BigDecimal(amount),
                "currencyCode", "USD"
            ))
            .exchange()
            .expectStatus().isCreated()
            .expectBody(TransferResponse.class)
            .returnResult()
            .getResponseBody();
        return response.id();
    }

    private List<FraudAlertResponse> alertsFor(UUID accountId) {
        return client
            .get()
            .uri("/api/fraud-alerts/{id}", accountId)
            .exchange()
            .expectStatus().isOk()
            .expectBody(new ParameterizedTypeReference<List<FraudAlertResponse>>() {})
            .returnResult()
            .getResponseBody();
    }

    private List<FraudAlertResponse> awaitAlerts(UUID accountId) {
        long deadline = System.currentTimeMillis() + 10_000;
        List<FraudAlertResponse> alerts = List.of();
        while (System.currentTimeMillis() < deadline) {
            alerts = alertsFor(accountId);
            if (!alerts.isEmpty()) {
                return alerts;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return alerts;
    }

}
