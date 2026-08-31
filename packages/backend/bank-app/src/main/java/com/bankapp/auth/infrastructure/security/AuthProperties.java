package com.bankapp.auth.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("bankapp.auth")
record AuthProperties(
    String privateKey,
    String keyId,
    String issuer,
    Duration accessTokenTtl,
    Duration refreshTokenTtl
) {}
