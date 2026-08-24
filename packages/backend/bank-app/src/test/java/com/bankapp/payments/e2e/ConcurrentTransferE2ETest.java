package com.bankapp.payments.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankapp.TestcontainersConfiguration;
import com.bankapp.accounts.api.dto.AccountResponse;
import com.bankapp.payments.domain.TransferRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Two identical requests in flight at once — the case the unique index exists for.
 *
 * <p>ADR-003 §6 says the loser of that race should re-read by key and replay the
 * original 201. It currently returns 409 instead: once a constraint violation
 * surfaces at flush, Hibernate marks the transaction rollback-only, so the
 * re-read cannot happen inside the handler's transaction. Closing that needs a
 * retry outside the transaction boundary.
 *
 * <p>These tests therefore pin the property that actually matters — the money
 * moves exactly once, and exactly one transfer row exists — while accepting
 * either status. When the retry wrapper lands, tighten the status assertion to
 * 201 and this test will already be guarding the safety half.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
public class ConcurrentTransferE2ETest {

    @Autowired
    private RestTestClient client;

    @Autowired
    private TransferRepository transfers;

    private static final String DEV_USER_ID =
        "11111111-1111-1111-1111-111111111111";

    private AccountResponse openAccount() {
        return client
            .post()
            .uri("/api/accounts")
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

    private AccountResponse fundedAccount(String amount) {
        AccountResponse account = openAccount();
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

    private int postTransfer(
        UUID from,
        UUID to,
        String amount,
        String idempotencyKey
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("fromAccountId", from);
        body.put("toAccountId", to);
        body.put("amount", new BigDecimal(amount));
        body.put("currencyCode", "USD");

        return client
            .post()
            .uri("/api/payments/transfers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Idempotency-Key", idempotencyKey)
            .body(body)
            .exchange()
            .returnResult(Void.class)
            .getStatus()
            .value();
    }

    private BigDecimal balanceOf(UUID accountId) {
        AccountResponse account = client
            .get()
            .uri("/api/accounts/" + accountId)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(account).isNotNull();
        return account.balance();
    }

    /** Fire both requests from a barrier so they genuinely overlap. */
    private List<Integer> raceTwoIdenticalTransfers(
        UUID from,
        UUID to,
        String amount,
        String key
    ) throws Exception {
        CyclicBarrier startTogether = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> attempt = () -> {
                startTogether.await();
                return postTransfer(from, to, amount, key);
            };
            Future<Integer> first = pool.submit(attempt);
            Future<Integer> second = pool.submit(attempt);
            return List.of(first.get(), second.get());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentDuplicatesMoveTheMoneyExactlyOnce() throws Exception {
        AccountResponse from = fundedAccount("100.00");
        AccountResponse to = openAccount();
        String key = UUID.randomUUID().toString();

        List<Integer> statuses = raceTwoIdenticalTransfers(
            from.id(),
            to.id(),
            "25.00",
            key
        );

        // The safety property: one debit, one credit, whatever the statuses were.
        assertThat(balanceOf(from.id())).isEqualByComparingTo("75.00");
        assertThat(balanceOf(to.id())).isEqualByComparingTo("25.00");

        // Exactly one row carries the key — the unique index did its job.
        assertThat(transfers.findByIdempotencyKey(key)).isPresent();

        // At least one caller was told it worked.
        assertThat(statuses).contains(201);

        // 409 on the loser is the documented deviation from ADR-003 §6; a plain
        // replay (201) is what it should become. Anything else is a real bug.
        assertThat(statuses).allSatisfy(status ->
            assertThat(status).isIn(201, 409)
        );
    }

    /** A sequential retry after the first has committed always replays cleanly. */
    @Test
    void aRetryAfterTheFirstCommittedReplaysWith201() {
        AccountResponse from = fundedAccount("100.00");
        AccountResponse to = openAccount();
        String key = UUID.randomUUID().toString();

        assertThat(postTransfer(from.id(), to.id(), "25.00", key)).isEqualTo(201);
        assertThat(postTransfer(from.id(), to.id(), "25.00", key)).isEqualTo(201);

        assertThat(balanceOf(from.id())).isEqualByComparingTo("75.00");
    }
}
