package com.bankapp.auth.domain;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(Email email);

    /** Refresh loads the user to read their current role — the only source of one. */
    Optional<User> findById(UUID id);
}
