// auth/infrastructure/security/BCryptPasswordHasher.java
package com.bankapp.auth.infrastructure.security;

import com.bankapp.auth.application.port.PasswordHasher;
import com.bankapp.auth.domain.PasswordHash;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BCryptPasswordHasher implements PasswordHasher {

    private static final int MAX_BYTES = 72;

    private final PasswordEncoder encoder;
    private final PasswordHash dummyHash;

    BCryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
        // Computed once, at startup: a real BCrypt hash of a value nobody knows and
        // nobody will ever send. Costs one ~250ms hash on boot.
        //
        // Deliberately generated rather than pasted in as a constant — a literal
        // "$2b$12$..." string sitting in source reads like a leaked credential to
        // anyone scanning the repo, and will be reported as one.
        this.dummyHash = new PasswordHash(encoder.encode(UUID.randomUUID().toString()));
    }

    @Override
    public PasswordHash hash(String plaintext) {
        // BCrypt silently ignores everything past 72 BYTES. The DTO bounds
        // characters, which is not the same thing the moment a password contains
        // anything non-ASCII — "🔐" is one character and four bytes. Silently
        // truncating a credential is worse than a 400.
        if (plaintext.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new IllegalArgumentException("password must be at most 72 bytes");
        }
        return new PasswordHash(encoder.encode(plaintext));
    }

    @Override
    public boolean matches(String plaintext, PasswordHash hash) {
        return encoder.matches(plaintext, hash.value());
    }

    @Override
    public PasswordHash dummyHash() {
        return dummyHash;
    }
}
