package com.bankapp.auth.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
// Jackson 3 (Boot 4). The Jackson 2 coordinates — com.fasterxml.jackson.databind —
// are not on this classpath; only jackson-annotations still lives there.
import tools.jackson.databind.ObjectMapper;

/**
 * Authentication fails inside the filter chain, before any controller runs, so
 * GlobalExceptionHandler never sees it. Without this, the 401 from a missing
 * token would be the only error in this API that isn't RFC 9457 — Spring
 * Security's default writes an empty body and a WWW-Authenticate header.
 *
 * <p>Same title and detail as GlobalExceptionHandler's 401, so a caller cannot
 * tell a rejected token from rejected credentials.
 */
@Component
class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    ProblemDetailAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * {@code authException} is deliberately unused, for the same reason
     * {@code onUnauthorized} ignores its exception: Spring Security's messages are
     * precise ("Jwt expired at ...", "An error occurred while attempting to decode
     * the Jwt"), and precision is exactly what must not reach an unauthenticated
     * caller.
     */
    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Unauthorized");
        problem.setDetail("Invalid credentials");

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
