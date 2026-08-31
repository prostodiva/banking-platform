package com.bankapp.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private Email email;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "password_hash", nullable = false, length = 60)
    private PasswordHash passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
        // required by JPA; protected so application code can't create blank users
    }

    private User(Email email, String fullName, PasswordHash passwordHash) {
        if (email == null) {
            throw new IllegalArgumentException("email is required");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("fullName is required");
        }
        if (passwordHash == null) {
            throw new IllegalArgumentException("passwordHash is required");
        }

        this.id = UUID.randomUUID();
        this.email = email;
        this.fullName = fullName.trim();
        this.passwordHash = passwordHash;
        this.role = Role.CUSTOMER;
        this.createdAt = Instant.now();
    }

    /**
     * The only way to create a user, and it can only produce a CUSTOMER
     * (ADR-004 decision 5). There is no parameter for the role, so no caller —
     * controller, handler, or a future refactor — has one to pass through.
     */
    public static User register(Email email, String fullName, PasswordHash passwordHash) {
        return new User(email, fullName, passwordHash);
    }

    public UUID getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public PasswordHash getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
