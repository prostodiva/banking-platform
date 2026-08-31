
package com.bankapp.auth.application.port;

import java.util.UUID;

public interface RefreshTokenIssuer {
    IssuedToken issue(UUID userId);
}
