package com.bankapp.shared.web;

import com.bankapp.shared.domain.EntityNotFoundException;
import com.bankapp.shared.domain.UnprocessableRequestException;
import com.bankapp.shared.domain.UnauthorizedException;

import java.util.stream.Collectors;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * Ordered ahead of Boot's own advice on purpose. {@code
 * spring.mvc.problemdetails.enabled=true} registers a {@code
 * ProblemDetailsExceptionHandler} that already handles every standard Spring MVC
 * exception, {@link MethodArgumentNotValidException} included. An unordered
 * {@code @RestControllerAdvice} sits at {@code LOWEST_PRECEDENCE} and loses that
 * race, so without this annotation {@link #onValidationFailure} is dead code and
 * a failed {@code @Valid} answers with Boot's field-less "Invalid request
 * content." — a 400 either way, which is why the E2E tests never caught it.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail onValidationFailure(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        problem.setDetail(
            ex
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(
                    error -> error.getField() + ": " + error.getDefaultMessage()
                )
                .collect(Collectors.joining("; "))
        );
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail onIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail onIllegalState(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Operation not allowed in current state");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail onDataIntegrityViolation(DataIntegrityViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Data conflict");
        problem.setDetail(
            "The request conflicts with existing data " +
                "(e.g. unknown owner or duplicate value)."
        );
        return problem;
    }

    /**
     * Someone else touched the same rows first. Two ways that happens here, both
     * retryable and both 409 rather than the 500 an unmapped infrastructure
     * exception would give:
     *
     * <ul>
     *   <li>{@code OptimisticLockingFailureException} — {@code @Version} caught a
     *       lost update (ADR-002).
     *   <li>{@code DeadlockLoserDataAccessException} — Postgres picked this
     *       transaction to kill. A transfer locks two `accounts` rows
     *       (ADR-003 §3), so deadlock is reachable, not theoretical.
     * </ul>
     *
     * Catching the shared parent covers both: the caller's instruction is the
     * same either way, which is "send it again".
     */
    @ExceptionHandler(ConcurrencyFailureException.class)
    ProblemDetail onConcurrencyFailure(ConcurrencyFailureException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Concurrent modification");
        problem.setDetail(
            "The record was modified by another request. Retry the operation."
        );
        return problem;
    }

    @ExceptionHandler(UnprocessableRequestException.class)
    ProblemDetail onUnprocessableRequest(UnprocessableRequestException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(
            HttpStatus.UNPROCESSABLE_ENTITY
        );
        problem.setTitle("Request refused");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ProblemDetail onEntityNotFound(EntityNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not found");
        problem.setDetail(ex.getMessage());
        return problem;
    }


    /**
     * The only handler here that ignores the exception's own message.
     *
     * <p>Every other mapping passes {@code ex.getMessage()} through, because a
     * caller who sent a bad request is entitled to know which part was bad. An
     * unauthenticated caller is entitled to know nothing: the moment this detail
     * varies between "no such user" and "wrong password", the login endpoint becomes
     * a tool for enumerating registered addresses (docs/07). Refresh has the same
     * property across unknown, expired and reused tokens.
     *
     * <p>The wording is deliberately generic — "credentials", not "email or
     * password" — because two endpoints share it and refresh sends neither. Ignoring
     * the exception's message is what keeps every 401 in this API byte-identical
     * without anyone maintaining a shared constant.
     *
     * <p>No {@code WWW-Authenticate} header, though RFC 9110 says a 401 should carry
     * one. There is no challenge to issue — the client is not expected to retry with
     * an Authorization header; it is expected to show a login form.
     */
    @ExceptionHandler(UnauthorizedException.class)
    ProblemDetail onUnauthorized(UnauthorizedException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Unauthorized");
        problem.setDetail("Invalid credentials");
        return problem;
    }

}
