package com.bankapp.shared.web;

import com.bankapp.shared.domain.EntityNotFoundException;
import com.bankapp.shared.domain.UnprocessableRequestException;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
     * Someone else changed the same aggregate first and {@code @Version} caught it
     * (ADR-002). The request was well formed, so this is a 409 the caller may
     * simply retry — not the 500 an unmapped infrastructure exception would give.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail onOptimisticLockingFailure(OptimisticLockingFailureException ex) {
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
}
