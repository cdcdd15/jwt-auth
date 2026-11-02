package com.example.securitylearning.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import com.example.securitylearning.support.JwtKeyFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class JwtStartupTest {
    @ParameterizedTest
    @ValueSource(strings = {"missing-public-property", "missing-private-property", "missing-public-file", "missing-private-file",
        "invalid-public", "invalid-private", "weak-public", "weak-private", "mismatched-pair"})
    void unacceptableKeysFailContextStartup(String scenario) throws Exception {
        try (var keys = new JwtKeyFixture(); var other = new JwtKeyFixture()) {
            Map<String, String> properties = paths(keys);
            switch (scenario) {
                case "missing-public-property" -> properties.remove("app.jwt.public-key-path");
                case "missing-private-property" -> properties.remove("app.jwt.private-key-path");
                case "missing-public-file" -> Files.delete(keys.publicPath());
                case "missing-private-file" -> Files.delete(keys.privatePath());
                case "invalid-public" -> Files.writeString(keys.publicPath(), "not a public PEM");
                case "invalid-private" -> Files.writeString(keys.privatePath(), "not a private PEM");
                case "weak-public", "weak-private" -> {
                    var generator = KeyPairGenerator.getInstance("RSA");
                    generator.initialize(1024);
                    var weak = generator.generateKeyPair();
                    if (scenario.equals("weak-public")) overwritePem(keys.publicPath(), "PUBLIC KEY", weak.getPublic().getEncoded());
                    else overwritePem(keys.privatePath(), "PRIVATE KEY", weak.getPrivate().getEncoded());
                }
                case "mismatched-pair" -> properties.put("app.jwt.private-key-path", other.privatePath().toString());
                default -> throw new IllegalArgumentException("Unknown key scenario");
            }
            context(properties).run(application -> {
                assertThat(application).hasFailed();
                assertThat(application.getStartupFailure()).isNotNull();
                if (scenario.endsWith("property")) {
                    String name = scenario.contains("public") ? "public" : "private";
                    assertThat(application.getStartupFailure()).hasStackTraceContaining("app.jwt." + name + "-key-path");
                } else if (scenario.endsWith("file")) {
                    assertThat(application.getStartupFailure()).hasRootCauseInstanceOf(java.nio.file.NoSuchFileException.class);
                } else {
                    assertThat(application.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    if (scenario.startsWith("weak")) assertThat(application.getStartupFailure()).hasStackTraceContaining("2048");
                    if (scenario.equals("mismatched-pair")) assertThat(application.getStartupFailure()).hasStackTraceContaining("match the verification key");
                }
            });
        }
    }

    @Test
    void matchingDevelopmentKeysCreateOneEncoderAndDecoder() throws Exception {
        try (var keys = new JwtKeyFixture()) {
            context(paths(keys)).run(application -> {
                assertThat(application).hasNotFailed().hasSingleBean(JwtEncoder.class).hasSingleBean(JwtDecoder.class);
            });
        }
    }

    private static Map<String, String> paths(JwtKeyFixture keys) {
        var properties = new LinkedHashMap<String, String>();
        properties.put("app.jwt.public-key-path", keys.publicPath().toString());
        properties.put("app.jwt.private-key-path", keys.privatePath().toString());
        return properties;
    }

    private static WebApplicationContextRunner context(Map<String, String> properties) {
        return new WebApplicationContextRunner().withUserConfiguration(JwtDecoderConfig.class, JwtIssuerConfig.class)
            .withPropertyValues(properties.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toArray(String[]::new));
    }

    private static void overwritePem(Path path, String type, byte[] encoded) throws Exception {
        // Overwrite existing owner-only files; never create a private file with default permissions.
        String body = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded);
        Files.writeString(path, "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n");
    }
}
