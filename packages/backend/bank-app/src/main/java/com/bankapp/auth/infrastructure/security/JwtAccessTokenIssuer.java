
package com.bankapp.auth.infrastructure.security;

import com.bankapp.auth.application.port.AccessTokenIssuer;
import com.bankapp.auth.application.port.IssuedToken;
import com.bankapp.auth.domain.Role;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder encoder;
    private final AuthProperties properties;

    JwtAccessTokenIssuer(JwtEncoder encoder, AuthProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    public IssuedToken issue(UUID userId, Role role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.accessTokenTtl());

        // A JWT is signed, not encrypted. Everything here is readable by anyone
        // holding the token, so: an opaque id and what the holder may do — never
        // who they are. No email, no name, no account numbers, no balances.
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .audience(List.of(properties.audience()))
            .subject(userId.toString())
            .issuedAt(now)
            .expiresAt(expiresAt)
            .id(UUID.randomUUID().toString())
            .claim("roles", List.of(role.name()))
            .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
            .keyId(properties.keyId())
            .build();

        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, expiresAt);
    }
}
