package com.example.securitylearning.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import com.example.securitylearning.api.AuthController.LoginRequest;
import com.example.securitylearning.support.JwtKeyFixture;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IssuerSecretHandlingTest {

    @Test
    void requestAndResponseDisplayAreRedacted() {
        assertThat(new LoginRequest("user", "private-placeholder").toString()).isEqualTo("LoginRequest[redacted]");
        assertThat(new JwtTokenService.TokenResponse("token-placeholder", "Bearer", 300).toString())
            .isEqualTo("TokenResponse[redacted]");
    }

    @Test
    void temporaryKeysAreOwnerOnlyAndRemoved() throws Exception {
        Path publicPath;
        Path privatePath;
        try (var fixture = new JwtKeyFixture()) {
            publicPath = fixture.publicPath();
            privatePath = fixture.privatePath();
            assertThat(Files.getPosixFilePermissions(privatePath)).isEqualTo(PosixFilePermissions.fromString("rw-------"));
            assertThat(Files.getPosixFilePermissions(privatePath.getParent())).isEqualTo(PosixFilePermissions.fromString("rwx------"));
        }
        assertThat(Files.exists(privatePath)).isFalse();
        assertThat(Files.exists(publicPath)).isFalse();
        assertThat(Files.exists(privatePath.getParent())).isFalse();
    }
}
