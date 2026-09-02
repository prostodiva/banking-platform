
package com.bankapp.auth.api;

import com.bankapp.auth.api.dto.AuthResponse;
import com.bankapp.auth.api.dto.LoginRequest;
import com.bankapp.auth.api.dto.LogoutRequest;
import com.bankapp.auth.api.dto.RefreshRequest;
import com.bankapp.auth.api.dto.RegisterRequest;
import com.bankapp.auth.application.AuthTokens;
import com.bankapp.auth.application.login.LoginCommand;
import com.bankapp.auth.application.login.LoginHandler;
import com.bankapp.auth.application.logout.LogoutCommand;
import com.bankapp.auth.application.logout.LogoutHandler;
import com.bankapp.auth.application.refreshsession.RefreshSessionCommand;
import com.bankapp.auth.application.refreshsession.RefreshSessionHandler;
import com.bankapp.auth.application.registeruser.RegisterUserCommand;
import com.bankapp.auth.application.registeruser.RegisterUserHandler;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserHandler registerUser;
    private final LoginHandler login;
    private final RefreshSessionHandler refreshSession;
    private final LogoutHandler logoutHandler;

    public AuthController(
        RegisterUserHandler registerUser,
        LoginHandler login,
        RefreshSessionHandler refreshSession,
        LogoutHandler logoutHandler
    ) {
        this.registerUser = registerUser;
        this.login = login;
        this.refreshSession = refreshSession;
        this.logoutHandler = logoutHandler;
    }

    /**
     * 201 with no Location header, unlike every other create in this API: there
     * is no GET for a user, and a Location that 404s is worse than none.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthTokens tokens = registerUser.handle(
            new RegisterUserCommand(request.email(), request.fullName(), request.password())
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.from(tokens));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = login.handle(
            new LoginCommand(request.email(), request.password())
        );

        return ResponseEntity.ok(AuthResponse.from(tokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthTokens tokens = refreshSession.handle(
            new RefreshSessionCommand(request.refreshToken())
        );

        return ResponseEntity.ok(AuthResponse.from(tokens));
    }

    /**
     * The first endpoint whose caller is known. {@code jwt} is the validated access
     * token and {@code sub} is the identity — which is why {@link LogoutRequest}
     * has no userId field for a client to claim.
     *
     * <p>{@code UUID.fromString} cannot fail in practice: every {@code sub} this
     * system mints is a UUID, and a token it did not mint fails the signature check
     * before reaching here. If it somehow did, {@code IllegalArgumentException}
     * maps to 400, which is the right answer for a malformed token.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody LogoutRequest request
    ) {
        logoutHandler.handle(
            new LogoutCommand(UUID.fromString(jwt.getSubject()), request.refreshToken())
        );

        return ResponseEntity.noContent().build();
    }

}
