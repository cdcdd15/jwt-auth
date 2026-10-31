package com.example.securitylearning.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public final class RsaKeyGenerator {

    private RsaKeyGenerator() {
    }

    public static KeyPair generateDevelopmentKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("RSA key generation is unavailable", exception);
        }
    }
}
