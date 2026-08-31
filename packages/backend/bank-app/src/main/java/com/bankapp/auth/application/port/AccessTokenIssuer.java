
package com.bankapp.auth.application.port;

import com.bankapp.auth.domain.Role;
import java.util.UUID;

public interface AccessTokenIssuer {
    IssuedToken issue(UUID userId, Role role);
}
