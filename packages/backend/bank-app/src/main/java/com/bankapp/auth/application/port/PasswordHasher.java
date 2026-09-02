package com.bankapp.auth.application.port;

import com.bankapp.auth.domain.PasswordHash;

public interface PasswordHasher {

    PasswordHash hash(String plaintext);

    boolean matches(String plaintext, PasswordHash hash);
    PasswordHash dummyHash();
}
