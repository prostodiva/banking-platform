
package com.bankapp.auth.infrastructure.persistence;

import com.bankapp.auth.domain.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserJpaRepository extends JpaRepository<User, UUID> {}
