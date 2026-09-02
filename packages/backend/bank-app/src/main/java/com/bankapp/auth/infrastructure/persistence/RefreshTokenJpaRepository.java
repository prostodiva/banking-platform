package com.bankapp.auth.infrastructure.persistence;

import com.bankapp.auth.domain.RefreshToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(UUID userId);
}
