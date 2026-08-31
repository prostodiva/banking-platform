// auth/infrastructure/security/BCryptPasswordHasher.java
package com.bankapp.auth.infrastructure.security;

import com.bankapp.auth.application.port.PasswordHasher;
import com.bankapp.auth.domain.PasswordHash;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BCryptPasswordHasher implements PasswordHasher {

    private static final int MAX_BYTES = 72;

    private final PasswordEncoder encoder;

    BCryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
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
}
