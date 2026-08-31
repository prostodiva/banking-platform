
package com.bankapp.auth.infrastructure.persistence;

import com.bankapp.auth.domain.RefreshToken;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, UUID> {}
