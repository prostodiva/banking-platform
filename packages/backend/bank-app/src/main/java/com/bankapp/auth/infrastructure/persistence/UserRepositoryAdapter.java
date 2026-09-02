
package com.bankapp.auth.infrastructure.persistence;

import com.bankapp.auth.domain.Email;
import com.bankapp.auth.domain.User;
import com.bankapp.auth.domain.UserRepository;
import com.bankapp.auth.domain.exceptions.EmailAlreadyRegisteredException;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * {@code saveAndFlush}, not {@code save}: plain save only queues the INSERT,
     * so a duplicate email would surface at commit — after the handler has
     * returned and outside anywhere that could translate it. Flushing here moves
     * the constraint violation to a line we are standing on.
     */
    @Override
    public User save(User user) {
        try {
            return jpa.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpa.findByEmail(email);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id);
    }
}
