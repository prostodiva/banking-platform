package com.bankapp.reports.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankapp.AbstractE2ETest;
import com.bankapp.accounts.api.dto.AccountResponse;
import com.bankapp.auth.api.dto.AuthResponse;
import com.bankapp.reports.api.dto.StatementResponse;
import com.bankapp.reports.api.dto.TransactionEntryResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class ReportsE2ETest extends AbstractE2ETest {

    private static final String PASSWORD = "correct horse battery";

    @Autowired
    private RestTestClient client;

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

    private void deposit(UUID accountId, String amount) {
        client
            .post()
            .uri("/api/accounts/" + accountId + "/deposit")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("amount", new BigDecimal(amount), "currencyCode", "USD"))
            .exchange()
            .expectStatus()
            .isOk();
    }

    private void withdraw(UUID accountId, String amount) {
        client
            .post()
            .uri("/api/accounts/" + accountId + "/withdraw")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("amount", new BigDecimal(amount), "currencyCode", "USD"))
            .exchange()
            .expectStatus()
            .isOk();
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

    private RestTestClient.ResponseSpec statementResponse(
        String accessToken, UUID accountId, YearMonth month
    ) {
        return client
            .get()
            .uri("/api/reports/statements?accountId=" + accountId + "&month=" + month)
            .header("Authorization", "Bearer " + accessToken)
            .exchange();
    }

    private StatementResponse statement(String accessToken, UUID accountId, YearMonth month) {
        return statementResponse(accessToken, accountId, month)
            .expectStatus()
            .isOk()
            .expectBody(StatementResponse.class)
            .returnResult()
            .getResponseBody();
    }

    private RestTestClient.ResponseSpec historyResponse(
        String accessToken, UUID accountId, LocalDate from, LocalDate to
    ) {
        return client
            .get()
            .uri("/api/reports/transactions?accountId=" + accountId + "&from=" + from + "&to=" + to)
            .header("Authorization", "Bearer " + accessToken)
            .exchange();
    }

    private List<TransactionEntryResponse> history(
        String accessToken, UUID accountId, LocalDate from, LocalDate to
    ) {
        return historyResponse(accessToken, accountId, from, to)
            .expectStatus()
            .isOk()
            .expectBody(new ParameterizedTypeReference<List<TransactionEntryResponse>>() {})
            .returnResult()
            .getResponseBody();
    }

    private List<TransactionEntryResponse> awaitHistory(
        String accessToken, UUID accountId, LocalDate from, LocalDate to, int expectedCount
    ) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        List<TransactionEntryResponse> entries = List.of();
        while (System.currentTimeMillis() < deadline) {
            entries = history(accessToken, accountId, from, to);
            if (entries.size() == expectedCount) {
                return entries;
            }
            Thread.sleep(200);
        }
        return entries;
    }

    @Test
    void statementShowsDepositsWithdrawalsAndReconciles() {
        AuthResponse user = register();
        AccountResponse account = openAccount(user.userId());
        deposit(account.id(), "1000");
        withdraw(account.id(), "250");

        StatementResponse statement = statement(user.accessToken(), account.id(), YearMonth.now());

        assertThat(statement).isNotNull();
        assertThat(statement.openingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(statement.totalIn()).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(statement.totalOut()).isEqualByComparingTo(new BigDecimal("250"));
        assertThat(statement.closingBalance()).isEqualByComparingTo(new BigDecimal("750"));
        // NFR-REP-01: opening + in − out = closing
        assertThat(
            statement.openingBalance().add(statement.totalIn()).subtract(statement.totalOut())
        ).isEqualByComparingTo(statement.closingBalance());
        assertThat(statement.currencyCode()).isEqualTo("USD");
        assertThat(statement.transactions()).hasSize(2);
        assertThat(statement.transactions().get(0).type()).isEqualTo("DEPOSIT");
        assertThat(statement.transactions().get(1).type()).isEqualTo("WITHDRAWAL");
    }

    @Test
    void statementForEmptyMonthIsZeroedAndEmpty() {
        AuthResponse user = register();
        AccountResponse account = openAccount(user.userId());
        deposit(account.id(), "500");

        StatementResponse statement = statement(
            user.accessToken(), account.id(), YearMonth.now().minusMonths(2)
        );

        assertThat(statement).isNotNull();
        assertThat(statement.transactions()).isEmpty();
        assertThat(statement.totalIn()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(statement.totalOut()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(statement.openingBalance()).isEqualByComparingTo(statement.closingBalance());
    }

    @Test
    void historyIsNewestFirst() {
        AuthResponse user = register();
        AccountResponse account = openAccount(user.userId());
        deposit(account.id(), "500");
        withdraw(account.id(), "100");
        deposit(account.id(), "50");

        List<TransactionEntryResponse> entries = history(
            user.accessToken(), account.id(),
            LocalDate.now().minusDays(7), LocalDate.now()
        );

        assertThat(entries).hasSize(3);
        assertThat(entries.get(0).type()).isEqualTo("DEPOSIT");
        assertThat(entries.get(0).amount()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(entries.get(1).type()).isEqualTo("WITHDRAWAL");
        assertThat(entries.get(2).type()).isEqualTo("DEPOSIT");
        assertThat(entries.get(2).amount()).isEqualByComparingTo(new BigDecimal("500"));
    }

    @Test
    void historyForEmptyRangeIsEmptyList() {
        AuthResponse user = register();
        AccountResponse account = openAccount(user.userId());
        deposit(account.id(), "500");

        List<TransactionEntryResponse> entries = history(
            user.accessToken(), account.id(),
            LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(2).plusDays(6)
        );

        assertThat(entries).isEmpty();
    }

    @Test
    void transferProjectsAnEntryOnBothAccounts() throws Exception {
        AuthResponse sender = register();
        AuthResponse receiver = register();
        AccountResponse from = openAccount(sender.userId());
        AccountResponse to = openAccount(receiver.userId());
        deposit(from.id(), "1000");

        transfer(from.id(), to.id(), "300");

        List<TransactionEntryResponse> senderEntries = awaitHistory(
            sender.accessToken(), from.id(),
            LocalDate.now().minusDays(7), LocalDate.now(), 2
        );
        assertThat(senderEntries).hasSize(2);
        TransactionEntryResponse out = senderEntries.stream()
            .filter(e -> e.type().equals("TRANSFER_OUT"))
            .findFirst()
            .orElseThrow();
        assertThat(out.amount()).isEqualByComparingTo(new BigDecimal("300"));
        assertThat(out.counterpartyAccountId()).isEqualTo(to.id());

        List<TransactionEntryResponse> receiverEntries = awaitHistory(
            receiver.accessToken(), to.id(),
            LocalDate.now().minusDays(7), LocalDate.now(), 1
        );
        assertThat(receiverEntries).hasSize(1);
        assertThat(receiverEntries.get(0).type()).isEqualTo("TRANSFER_IN");
        assertThat(receiverEntries.get(0).counterpartyAccountId()).isEqualTo(from.id());
    }

    @Test
    void futureMonthIsRejected() {
        AuthResponse user = register();
        AccountResponse account = openAccount(user.userId());

        statementResponse(user.accessToken(), account.id(), YearMonth.now().plusMonths(1))
            .expectStatus()
            .isBadRequest();
    }

    @Test
    void fromAfterToIsRejected() {
        AuthResponse user = register();
        AccountResponse account = openAccount(user.userId());

        historyResponse(
            user.accessToken(), account.id(),
            LocalDate.now(), LocalDate.now().minusDays(1)
        )
            .expectStatus()
            .isBadRequest();
    }

    @Test
    void otherCustomersAccountIsNotFound() {
        AuthResponse owner = register();
        AuthResponse other = register();
        AccountResponse account = openAccount(owner.userId());
        deposit(account.id(), "500");

        statementResponse(other.accessToken(), account.id(), YearMonth.now())
            .expectStatus()
            .isNotFound();
        historyResponse(
            other.accessToken(), account.id(),
            LocalDate.now().minusDays(7), LocalDate.now()
        )
            .expectStatus()
            .isNotFound();
    }

    @Test
    void unknownAccountIsNotFound() {
        AuthResponse user = register();

        statementResponse(user.accessToken(), UUID.randomUUID(), YearMonth.now())
            .expectStatus()
            .isNotFound();
    }

    @Test
    void reportsRequireAuthentication() {
        UUID accountId = UUID.randomUUID();

        client
            .get()
            .uri("/api/reports/statements?accountId=" + accountId + "&month=" + YearMonth.now())
            .exchange()
            .expectStatus()
            .isUnauthorized();
        client
            .get()
            .uri("/api/reports/transactions?accountId=" + accountId
                + "&from=" + LocalDate.now().minusDays(7) + "&to=" + LocalDate.now())
            .exchange()
            .expectStatus()
            .isUnauthorized();
    }
}
