package com.bankapp.auth.domain;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);
}
