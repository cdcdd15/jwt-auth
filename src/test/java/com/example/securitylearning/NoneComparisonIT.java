package com.example.securitylearning;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "app.security-mode=none")
class NoneComparisonIT extends AbstractSecurityComparison {
}
