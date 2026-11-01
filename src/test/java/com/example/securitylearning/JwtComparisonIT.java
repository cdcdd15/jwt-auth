package com.example.securitylearning;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.example.securitylearning.support.JwtKeyFixture;
import org.junit.jupiter.api.AfterAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "app.security-mode=jwt")
@Import(JwtComparisonIT.FixedTimeConfiguration.class)
class JwtComparisonIT extends AbstractSecurityComparison {
    private static final JwtKeyFixture KEYS = new JwtKeyFixture();

    @DynamicPropertySource
    static void keyPaths(DynamicPropertyRegistry properties) {
        properties.add("app.jwt.public-key-path", () -> KEYS.publicPath().toString());
        properties.add("app.jwt.private-key-path", () -> KEYS.privatePath().toString());
    }

    @AfterAll
    static void removeKeys() throws IOException {
        KEYS.close();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedTimeConfiguration {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2030-01-01T12:00:00Z"), ZoneOffset.UTC);
        }
    }
}
