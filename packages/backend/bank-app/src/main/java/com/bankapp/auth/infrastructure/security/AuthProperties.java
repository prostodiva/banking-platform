package com.bankapp.auth.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("bankapp.auth")
record AuthProperties(
    String privateKey,
    String keyId,
    String issuer,
    /**
     * Read by both halves: the issuer puts it in {@code aud}, the decoder requires
     * it. It was a literal inside {@code JwtAccessTokenIssuer} until the decoder
     * needed the same string, and two literals that must match are one too many.
     */
    String audience,
    Duration accessTokenTtl,
    Duration refreshTokenTtl
) {}
