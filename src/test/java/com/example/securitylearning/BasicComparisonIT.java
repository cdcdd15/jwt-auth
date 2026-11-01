package com.example.securitylearning;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "app.security-mode=basic")
class BasicComparisonIT extends AbstractSecurityComparison {
}
