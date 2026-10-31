# Proposed Project Structure

P1.S1 now implements the files listed below. The larger layout remains a **target**, not a claim that future classes exist.

```text
src/main/java/com/example/securitylearning/SecurityLearningApplication.java
src/main/java/com/example/securitylearning/api/DemoController.java
src/test/java/com/example/securitylearning/PlainMvcIT.java
docs/learning/P1.S1-WALKTHROUGH.md
```

```text
jwt-auth/
├── pom.xml
├── README.md
├── AGENTS.md
├── ROADMAP.md
├── docs/
│   ├── PROJECT-STRUCTURE.md
│   ├── LEARNING-SUPPORT-REQUIREMENTS.md
│   ├── FLOWS-COMPARED.md
│   ├── TEST-MATRIX.md
│   └── roadmap/
│       ├── P1-PLAIN-MVC.md
│       ├── P2-BASIC-SECURITY.md
│       ├── P3-JWT-RS256.md
│       ├── P4-JWT-VALIDATION.md
│       └── P5-COMPARISON.md
└── src/                          # P1.S1 application, controller, and HTTP tests
```

## Target Java and test layout (mostly future work)

```text
src/
├── main/java/.../
│   ├── SecurityLearningApplication.java
│   ├── api/
│   │   ├── DemoController.java
│   │   └── AuthController.java
│   ├── config/
│   │   ├── SecurityModeConfig.java
│   │   ├── BasicSecurityConfig.java
│   │   └── JwtSecurityConfig.java
│   ├── security/
│   │   ├── JwtTokenService.java
│   │   └── JwtAuthenticationFilter.java  # ORIGINAL proposal only; DO NOT implement
│   └── tracing/
│       ├── RequestTraceFilter.java
│       └── MvcTraceInterceptor.java
└── test/java/.../
    ├── PlainMvcIT.java
    ├── BasicSecurityIT.java
    ├── JwtLoginIT.java
    ├── JwtValidationIT.java
    └── SecurityComparisonIT.java
```

**Revised architecture:** replace the originally proposed `JwtAuthenticationFilter.java` with Spring Security's built-in `BearerTokenAuthenticationFilter`, `JwtAuthenticationProvider`, and `NimbusJwtDecoder`; create a `JwtAuthenticationConverter` configuration as necessary. The built-in filter is framework code, not a project class. Add `JwtKeyConfig`/`JwtDecoderConfig` as appropriate. Keep target class names flexible when framework-accurate alternatives are better.

## Development sequencing
P1 initially has no Spring Security on the classpath. When adding P2, preserve the P1 experience through explicit security-filter registration isolation or a separate build profile; a `permitAll` chain is not equivalent to no Spring Security. Do not hardcode security-specific servlet filters into the P1 path.
