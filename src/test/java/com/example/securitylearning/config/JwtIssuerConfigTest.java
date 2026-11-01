package com.example.securitylearning.config;

import java.nio.file.Files;
import java.security.interfaces.RSAPublicKey;

import com.example.securitylearning.support.JwtKeyFixture;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtIssuerConfigTest {

    @Test
    void mismatchedSigningKeyFailsInsteadOfIssuingUnverifiableTokens() throws Exception {
        try (var trusted = new JwtKeyFixture(); var other = new JwtKeyFixture()) {
            assertThatThrownBy(() -> new JwtIssuerConfig().jwtEncoder(other.privatePath().toString(),
                (RSAPublicKey) trusted.keys().getPublic()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("match the verification key");
        }
    }

    @Test
    void invalidPrivateKeyFails() throws Exception {
        try (var fixture = new JwtKeyFixture()) {
            Files.writeString(fixture.privatePath(), "not a private key");
            assertThatThrownBy(() -> new JwtIssuerConfig().jwtEncoder(fixture.privatePath().toString(),
                (RSAPublicKey) fixture.keys().getPublic())).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
