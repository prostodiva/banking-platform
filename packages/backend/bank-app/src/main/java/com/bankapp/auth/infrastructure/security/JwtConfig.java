package com.bankapp.auth.infrastructure.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
class JwtConfig {

    /**
     * One environment variable, two keys. A PKCS#8 RSA private key in CRT form
     * carries the modulus and the public exponent, so the public key is derived
     * rather than configured — which means the pair can never drift apart, and
     * there is no second variable for someone to set wrong.
     *
     * <p>Its own bean because the decoder needs the public half too, and deriving
     * it twice means two things that can drift.
     */
    @Bean
    RSAKey rsaKey(AuthProperties properties) throws Exception {
        byte[] der = Base64.getDecoder().decode(properties.privateKey());
        KeyFactory rsa = KeyFactory.getInstance("RSA");

        RSAPrivateCrtKey privateKey =
            (RSAPrivateCrtKey) rsa.generatePrivate(new PKCS8EncodedKeySpec(der));
        RSAPublicKey publicKey = (RSAPublicKey) rsa.generatePublic(
            new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent())
        );

        return new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(properties.keyId())
            .build();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey rsaKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    /**
     * Four checks, not one. A signature proves this key signed the token — it says
     * nothing about when, for whom, or by whom.
     *
     * <p>Spelled out rather than {@code JwtValidators.createDefault()}, which gives
     * timestamp validation and nothing else. The two it omits are the two that
     * matter once more than one system signs tokens, and <b>audience is the one
     * people skip</b>: a token minted by this issuer for a different service is
     * perfectly valid and correctly signed, and accepting it means any sibling
     * service can hand us a token that works.
     *
     * <p>There is still no JWKS endpoint. ADR-004 commits to publishing one; in a
     * single process the decoder holds the key directly and nothing would read it.
     */
    @Bean
    JwtDecoder jwtDecoder(RSAKey rsaKey, AuthProperties properties) throws Exception {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withPublicKey(rsaKey.toRSAPublicKey())
            // Pinned, never read from the token's own `alg` header. A verifier that
            // asks the token how to check it is the `alg: none` family of attacks.
            .signatureAlgorithm(SignatureAlgorithm.RS256)
            .build();

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            new JwtIssuerValidator(properties.issuer()),
            new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD,
                audience -> audience != null && audience.contains(properties.audience())
            )
        ));

        return decoder;
    }

    /**
     * Cost 12: roughly 250ms per hash on current hardware. Deliberately slow, so
     * an attacker with a stolen table gets a handful of guesses per second per
     * core instead of billions.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
