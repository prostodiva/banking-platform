package com.bankapp.auth.infrastructure.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
class JwtConfig {

    /**
     * One environment variable, two keys. A PKCS#8 RSA private key in CRT form
     * carries the modulus and the public exponent, so the public key is derived
     * rather than configured — which means the pair can never drift apart, and
     * there is no second variable for someone to set wrong.
     */
    @Bean
    JwtEncoder jwtEncoder(AuthProperties properties) throws Exception {
        byte[] der = Base64.getDecoder().decode(properties.privateKey());
        KeyFactory rsa = KeyFactory.getInstance("RSA");

        RSAPrivateCrtKey privateKey =
            (RSAPrivateCrtKey) rsa.generatePrivate(new PKCS8EncodedKeySpec(der));
        RSAPublicKey publicKey = (RSAPublicKey) rsa.generatePublic(
            new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent())
        );

        RSAKey jwk = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(properties.keyId())
            .build();

        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
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
