package com.example.securitylearning.config;

import com.example.securitylearning.support.JwtKeyFixture;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class JwtStartupTest {
    @ParameterizedTest
    @ValueSource(strings = {"public", "private"})
    void missingRequiredKeyFailsStartupWithoutModeSelection(String missingKey) throws Exception {
        try (var keys = new JwtKeyFixture()) {
            String suppliedKey = missingKey.equals("public") ? "private" : "public";
            String path = suppliedKey.equals("public") ? keys.publicPath().toString() : keys.privatePath().toString();
            new WebApplicationContextRunner().withUserConfiguration(JwtDecoderConfig.class, JwtIssuerConfig.class)
                .withPropertyValues("app.jwt." + suppliedKey + "-key-path=" + path)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("app.jwt." + missingKey + "-key-path");
                });
        }
    }
}
