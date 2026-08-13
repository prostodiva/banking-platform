package com.bankapp.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankapp.accounts.domain.exceptions.AccountNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
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
}
