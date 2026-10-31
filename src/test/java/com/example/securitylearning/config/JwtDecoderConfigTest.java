package com.example.securitylearning.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtDecoderConfigTest {

    @TempDir
    Path directory;

    private final JwtDecoderConfig config = new JwtDecoderConfig();

    @Test
    void missingPublicKeyFailsInsteadOfGeneratingFallback() {
        assertThatThrownBy(() -> config.jwtVerificationKey(directory.resolve("missing.pem").toString()))
            .isInstanceOf(java.io.IOException.class);
    }

    @Test
    void invalidPublicKeyFails() throws Exception {
        Path path = directory.resolve("invalid.pem");
        Files.writeString(path, "not a public key");
        assertThatThrownBy(() -> config.jwtVerificationKey(path.toString()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void undersizedRsaPublicKeyFails() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        String encoded = Base64.getMimeEncoder(64, new byte[]{'\n'})
            .encodeToString(generator.generateKeyPair().getPublic().getEncoded());
        Path path = directory.resolve("weak-public.pem");
        Files.writeString(path, "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----\n");
        assertThatThrownBy(() -> config.jwtVerificationKey(path.toString()))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least 2048");
    }
}
