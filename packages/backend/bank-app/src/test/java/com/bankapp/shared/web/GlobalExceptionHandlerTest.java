package com.bankapp.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankapp.accounts.domain.exceptions.AccountNotFoundException;
import com.bankapp.payments.domain.exceptions.IdempotencyKeyConflictException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsEntityNotFoundTo404() {
        UUID accountId = UUID.randomUUID();

        ProblemDetail problem = handler.onEntityNotFound(
            new AccountNotFoundException(accountId)
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Not found");
        assertThat(problem.getDetail()).contains(accountId.toString());
    }

    /** ADR-003 §3: a lost-update collision is a retryable 409, not a 500. */
    @Test
    void mapsOptimisticLockingFailureTo409() {
        ProblemDetail problem = handler.onConcurrencyFailure(
            new OptimisticLockingFailureException("Row was updated by another transaction")
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Concurrent modification");
    }

    /**
     * A transfer locks two accounts rows, so Postgres can pick this transaction as
     * the deadlock victim. Same family, same retryable 409 — not a 500.
     */
    @Test
    void mapsDeadlockLoserTo409() {
        ProblemDetail problem = handler.onConcurrencyFailure(
            new DeadlockLoserDataAccessException("deadlock detected", null)
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Concurrent modification");
    }

    /** The detail must not leak the entity class and id Spring puts in the message. */
    @Test
    void concurrencyFailureDetailDoesNotEchoTheInternalMessage() {
        ProblemDetail problem = handler.onConcurrencyFailure(
            new OptimisticLockingFailureException(
                "Row was updated or deleted by another transaction " +
                "(com.bankapp.accounts.domain.Account#some-id)"
            )
        );

        assertThat(problem.getDetail()).doesNotContain("com.bankapp");
    }

    /** ADR-003 §7: key reuse across different requests is understood and refused. */
    @Test
    void mapsUnprocessableRequestTo422() {
        ProblemDetail problem = handler.onUnprocessableRequest(
            new IdempotencyKeyConflictException("idem-key-1")
        );

        assertThat(problem.getStatus()).isEqualTo(
            HttpStatus.UNPROCESSABLE_ENTITY.value()
        );
        assertThat(problem.getTitle()).isEqualTo("Request refused");
        assertThat(problem.getDetail()).contains("idem-key-1");
    }
}
