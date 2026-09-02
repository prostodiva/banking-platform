
package com.bankapp.auth.application.login;

import com.bankapp.auth.application.port.AccessTokenIssuer;
import com.bankapp.auth.application.port.DomainEventPublisher;
import com.bankapp.auth.application.port.IssuedToken;
import com.bankapp.auth.application.port.PasswordHasher;
import com.bankapp.auth.application.port.RefreshTokenIssuer;
import com.bankapp.auth.domain.Email;
import com.bankapp.auth.domain.PasswordHash;
import com.bankapp.auth.domain.User;
import com.bankapp.auth.domain.UserRepository;
import com.bankapp.auth.domain.events.UserLoggedIn;
import com.bankapp.auth.domain.exceptions.BadCredentialsException;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginHandler {

    private final UserRepository users;
    private final PasswordHasher passwords;
    private final AccessTokenIssuer accessTokens;
    private final RefreshTokenIssuer refreshTokens;
    private final DomainEventPublisher events;

    public LoginHandler(
        UserRepository users,
        PasswordHasher passwords,
        AccessTokenIssuer accessTokens,
        RefreshTokenIssuer refreshTokens,
        DomainEventPublisher events
    ) {
        this.users = users;
        this.passwords = passwords;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.events = events;
    }

    @Transactional
    public LoginResult handle(LoginCommand command) {
        Optional<User> found = users.findByEmail(new Email(command.email()));

        // Verify unconditionally. Not "if the user exists" — the whole point is
        // that an unknown address costs the same quarter-second as a wrong
        // password, so the response time says nothing about who is registered.
        PasswordHash hash = found
            .map(User::getPasswordHash)
            .orElseGet(passwords::dummyHash);

        boolean matched = passwords.matches(command.password(), hash);

        if (found.isEmpty() || !matched) {
            throw new BadCredentialsException();
        }

        User user = found.get();
        IssuedToken access = accessTokens.issue(user.getId(), user.getRole());
        IssuedToken refresh = refreshTokens.issue(user.getId());

        events.publish(new UserLoggedIn(user.getId(), Instant.now()));

        return new LoginResult(
            user.getId(),
            access.value(),
            refresh.value(),
            access.expiresAt()
        );
    }
}
