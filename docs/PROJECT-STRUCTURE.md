# Proposed Project Structure

P1–P3 and P4.S1 now implement the files listed below. The larger layout remains a **target**, not a claim that future classes exist.

```text
src/main/java/com/example/securitylearning/SecurityLearningApplication.java
src/main/java/com/example/securitylearning/api/DemoController.java
src/main/java/com/example/securitylearning/api/AuthController.java
src/main/java/com/example/securitylearning/config/InMemoryIdentityConfig.java
src/main/java/com/example/securitylearning/config/JwtIssuerConfig.java
src/main/java/com/example/securitylearning/security/JwtTokenService.java
src/main/java/com/example/securitylearning/security/RsaKeyGenerator.java
src/main/java/com/example/securitylearning/security/JwtClaimContractValidator.java
src/main/java/com/example/securitylearning/config/JwtDecoderConfig.java
src/main/java/com/example/securitylearning/config/JwtSecurityConfig.java
src/main/java/com/example/securitylearning/config/SecurityModeConfig.java
src/main/java/com/example/securitylearning/config/BasicSecurityConfig.java
src/main/java/com/example/securitylearning/tracing/TraceConfiguration.java
src/main/java/com/example/securitylearning/tracing/TraceEventSink.java
src/main/java/com/example/securitylearning/tracing/RequestTraceFilter.java
src/main/java/com/example/securitylearning/tracing/SecurityTraceFilter.java
src/main/java/com/example/securitylearning/tracing/MvcTraceInterceptor.java
src/test/java/com/example/securitylearning/PlainMvcIT.java
src/test/java/com/example/securitylearning/NoneSecurityIT.java
src/test/java/com/example/securitylearning/BasicSecurityIT.java  # includes test-only principal probe
src/test/java/com/example/securitylearning/SecurityModeConfigTest.java
src/test/java/com/example/securitylearning/security/RsaJwtTest.java
src/test/java/com/example/securitylearning/JwtValidationIT.java  # includes isolated test-only identity probe
src/test/java/com/example/securitylearning/JwtLoginIT.java
src/test/java/com/example/securitylearning/support/JwtKeyFixture.java
src/test/java/com/example/securitylearning/config/JwtIssuerConfigTest.java
src/test/java/com/example/securitylearning/security/IssuerSecretHandlingTest.java
src/test/java/com/example/securitylearning/security/JwtClaimContractValidatorTest.java
src/test/java/com/example/securitylearning/config/JwtDecoderConfigTest.java
src/test/java/com/example/securitylearning/RequestTraceIT.java
src/test/java/com/example/securitylearning/ErrorPathIT.java  # includes test-only error fixtures
src/test/java/com/example/securitylearning/tracing/TraceEventSinkTest.java
docs/learning/P1.S1-WALKTHROUGH.md
docs/learning/P1.S2-WALKTHROUGH.md
docs/learning/P1.S3-WALKTHROUGH.md
docs/learning/P2.S1-WALKTHROUGH.md
docs/learning/P2.S2-WALKTHROUGH.md
docs/learning/P2.S3-WALKTHROUGH.md
docs/learning/P3.S1-WALKTHROUGH.md
docs/learning/P3.S2-WALKTHROUGH.md
docs/learning/P3.S3-WALKTHROUGH.md
docs/learning/P4.S1-WALKTHROUGH.md
docs/JWT-CLAIM-CONTRACT.md
```

```text
jwt-auth/
├── pom.xml
├── README.md
├── AGENTS.md
├── ROADMAP.md
├── docs/
│   ├── IN-SCOPE-OUT-OF-SCOPE.md
│   ├── ROADMAP-REVIEW.md
│   ├── PROJECT-STRUCTURE.md
│   ├── LEARNING-SUPPORT-REQUIREMENTS.md
│   ├── FLOWS-COMPARED.md
│   ├── TEST-MATRIX.md
│   ├── JWT-CLAIM-CONTRACT.md
│   └── roadmap/
│       ├── P1-PLAIN-MVC.md
│       ├── P2-BASIC-SECURITY.md
│       ├── P3-JWT-RS256.md
│       ├── P4-JWT-VALIDATION.md
│       └── P5-COMPARISON.md
└── src/                          # P1/P2 complete; P3.S1–S3 validation foundation; application, tracing, mode configuration, and tests
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
│   │   └── JwtTokenService.java
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

**JWT architecture:** use Spring Security's built-in `BearerTokenAuthenticationFilter`, `JwtAuthenticationProvider`, and `NimbusJwtDecoder`; create a `JwtAuthenticationConverter` configuration as necessary. The built-in filter is framework code, not a project class. Add `JwtKeyConfig`/`JwtDecoderConfig` as appropriate. Keep target class names flexible when framework-accurate alternatives are better.

## Development sequencing
P1 originally had no Spring Security on the classpath. P2.S1 now includes security libraries in both modes but isolates registration: the application excludes Boot security/user defaults, conditional basic configuration enables security and registers the servlet proxy, and none creates neither a security chain nor its proxy. A `permitAll` chain is not used for none. See the P2.S1 walkthrough for exact bean/registration evidence.
