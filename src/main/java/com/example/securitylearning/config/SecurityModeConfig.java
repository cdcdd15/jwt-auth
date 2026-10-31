package com.example.securitylearning.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class SecurityModeConfig {

    public SecurityModeConfig(Environment environment) {
        String mode = environment.getProperty("app.security-mode", "none");
        if (!mode.equals("none") && !mode.equals("basic") && !mode.equals("jwt")) {
            throw new IllegalArgumentException("app.security-mode must be none, basic, or jwt");
        }
    }
}
