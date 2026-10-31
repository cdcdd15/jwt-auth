package com.example.securitylearning;

import com.example.securitylearning.config.SecurityModeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityModeConfigTest {

    @Test
    void missingModeDefaultsToNone() {
        assertThatCode(() -> new SecurityModeConfig(new MockEnvironment())).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"none", "basic"})
    void acceptsImplementedModes(String mode) {
        assertThatCode(() -> new SecurityModeConfig(
            new MockEnvironment().withProperty("app.security-mode", mode))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"jwt", "typo", "none,basic", ""})
    void rejectsUnsupportedOrCombinedModes(String mode) {
        assertThatThrownBy(() -> new SecurityModeConfig(
            new MockEnvironment().withProperty("app.security-mode", mode)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must be none or basic");
    }
}
