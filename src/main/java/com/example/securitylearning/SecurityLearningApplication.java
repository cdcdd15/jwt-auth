package com.example.securitylearning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;

@SpringBootApplication(exclude = SecurityFilterAutoConfiguration.class)
public class SecurityLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityLearningApplication.class, args);
    }
}
