
package com.bankapp.auth.api;

import com.bankapp.auth.api.dto.AuthResponse;
import com.bankapp.auth.api.dto.RegisterRequest;
import com.bankapp.auth.application.registeruser.RegisterUserCommand;
import com.bankapp.auth.application.registeruser.RegisterUserHandler;
import com.bankapp.auth.application.registeruser.RegisterUserResult;
import com.bankapp.auth.application.login.LoginCommand;
import com.bankapp.auth.application.login.LoginHandler;
import com.bankapp.auth.application.login.LoginResult;
import com.bankapp.auth.api.dto.LoginRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserHandler registerUser;
    private final LoginHandler login;

    public AuthController(RegisterUserHandler registerUser, LoginHandler login) {
        this.registerUser = registerUser;
        this.login = login;
    }

    /**
     * 201 with no Location header, unlike every other create in this API: there
     * is no GET for a user, and a Location that 404s is worse than none.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterUserResult result = registerUser.handle(
            new RegisterUserCommand(request.email(), request.fullName(), request.password())
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.from(result));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = login.handle(
            new LoginCommand(request.email(), request.password())
        );

        return ResponseEntity.ok(AuthResponse.from(result));
    }

}
