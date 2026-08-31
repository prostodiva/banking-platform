
package com.bankapp.auth.application.registeruser;

import com.bankapp.auth.application.port.AccessTokenIssuer;
import com.bankapp.auth.application.port.DomainEventPublisher;
import com.bankapp.auth.application.port.IssuedToken;
import com.bankapp.auth.application.port.PasswordHasher;
import com.bankapp.auth.application.port.RefreshTokenIssuer;
import com.bankapp.auth.domain.Email;
import com.bankapp.auth.domain.PasswordHash;
import com.bankapp.auth.domain.User;
import com.bankapp.auth.domain.UserRepository;
import com.bankapp.auth.domain.events.UserRegistered;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserHandler {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokens;
    private final RefreshTokenIssuer refreshTokens;
    private final DomainEventPublisher events;

    public RegisterUserHandler(
        UserRepository users,
        PasswordHasher passwordHasher,
        AccessTokenIssuer accessTokens,
        RefreshTokenIssuer refreshTokens,
        DomainEventPublisher events
    ) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.events = events;
    }

    @Transactional
    public RegisterUserResult handle(RegisterUserCommand command) {
        Email email = new Email(command.email());
        PasswordHash hash = passwordHasher.hash(command.password());

        User user = User.register(email, command.fullName(), hash);
        users.save(user);

        IssuedToken access = accessTokens.issue(user.getId(), user.getRole());
        IssuedToken refresh = refreshTokens.issue(user.getId());

        events.publish(new UserRegistered(user.getId(), user.getCreatedAt()));

        return new RegisterUserResult(
            user.getId(),
            access.value(),
            refresh.value(),
            access.expiresAt()
        );
    }
}
