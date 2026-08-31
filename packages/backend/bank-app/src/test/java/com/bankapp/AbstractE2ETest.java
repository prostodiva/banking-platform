package com.bankapp;

import java.security.KeyPairGenerator;
import java.util.Base64;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base for every end-to-end test. Exists because the JWT signing key has no
 * default (ADR-004 decision 2) and the context will not start without one — so
 * each test run mints a throwaway keypair in memory. Nothing to commit, nothing
 * to rotate, nothing that outlives the JVM.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
public abstract class AbstractE2ETest {

    private static final String PRIVATE_KEY = generatePrivateKey();

    @DynamicPropertySource
    static void authProperties(DynamicPropertyRegistry registry) {
        registry.add("bankapp.auth.private-key", () -> PRIVATE_KEY);
    }

    private static String generatePrivateKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return Base64.getEncoder()
                .encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException("could not generate a test signing key", e);
        }
    }
}
