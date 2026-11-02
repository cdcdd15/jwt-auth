# Implemented project structure

> Current migration checkpoint: [J1.S2](learning/J1.S2-WALKTHROUGH.md) makes JWT unconditional on Java17. None/basic configurations, mode selection and cross-mode suites are retired; 94 HTTP + 23 unit cases pass. Earlier three-mode descriptions below are historical and will be replaced in J3. The migration roadmap supersedes conflicting historical requirements.

> JWT-only migration checkpoint: [J1.S1](learning/J1.S1-WALKTHROUGH.md) now requires Java17 and passed 153 HTTP + 29 unit cases on Temurin17.0.21. Three-mode behavior remains until J1.S2; earlier Java21 instructions/results below are historical. [The migration roadmap](../ROADMAP.md) is authoritative.

All fourteen steps P1.S1–P5.S2 are complete. This inventory describes actual files, not proposed classes. [COMPLETE-GUIDE](learning/COMPLETE-GUIDE.md) contains the current production source/method audit; [final verification](learning/P5.S2-WALKTHROUGH.md) covers Java21 and the executed suites.

## Java sources

```text
src/main/java/com/example/securitylearning/SecurityLearningApplication.java
src/main/java/com/example/securitylearning/api/AuthController.java
src/main/java/com/example/securitylearning/api/DemoController.java
src/main/java/com/example/securitylearning/config/InMemoryIdentityConfig.java
src/main/java/com/example/securitylearning/config/JwtDecoderConfig.java
src/main/java/com/example/securitylearning/config/JwtIssuerConfig.java
src/main/java/com/example/securitylearning/config/JwtSecurityConfig.java
src/main/java/com/example/securitylearning/security/JwtClaimContractValidator.java
src/main/java/com/example/securitylearning/security/JwtTokenService.java
src/main/java/com/example/securitylearning/security/RsaKeyGenerator.java
src/main/java/com/example/securitylearning/security/StrictJwtDecoder.java
src/main/java/com/example/securitylearning/tracing/MvcTraceInterceptor.java
src/main/java/com/example/securitylearning/tracing/RequestTraceFilter.java
src/main/java/com/example/securitylearning/tracing/SecurityTraceFilter.java
src/main/java/com/example/securitylearning/tracing/TraceConfiguration.java
src/main/java/com/example/securitylearning/tracing/TraceEventSink.java
src/test/java/com/example/securitylearning/ErrorPathIT.java
src/test/java/com/example/securitylearning/JwtDiagnosticsIT.java
src/test/java/com/example/securitylearning/JwtLoginIT.java
src/test/java/com/example/securitylearning/JwtNegativeValidationIT.java
src/test/java/com/example/securitylearning/JwtValidationIT.java
src/test/java/com/example/securitylearning/JwtMvcIT.java
src/test/java/com/example/securitylearning/config/JwtStartupTest.java
src/test/java/com/example/securitylearning/support/JwtTestTokens.java
src/test/java/com/example/securitylearning/RequestTraceIT.java
src/test/java/com/example/securitylearning/config/JwtDecoderConfigTest.java
src/test/java/com/example/securitylearning/config/JwtIssuerConfigTest.java
src/test/java/com/example/securitylearning/security/IssuerSecretHandlingTest.java
src/test/java/com/example/securitylearning/security/JwtClaimContractValidatorTest.java
src/test/java/com/example/securitylearning/security/RsaJwtTest.java
src/test/java/com/example/securitylearning/support/JwtKeyFixture.java
src/test/java/com/example/securitylearning/tracing/TraceEventSinkTest.java
```

Test-only fixtures, failing controller, MVC/context probes and filter-chain observers live under src/test and are not packaged in the application. The abstract comparison contract is inherited by three mode-specific IT bootstraps. Tests use separate Spring contexts and actual random-port Tomcat HTTP.

## Learning package

```text
docs/learning/COMPLETE-GUIDE.md
docs/learning/J1.S1-WALKTHROUGH.md
docs/learning/J1.S2-WALKTHROUGH.md
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
docs/learning/P4.S2-WALKTHROUGH.md
docs/learning/P4.S3-WALKTHROUGH.md
docs/learning/P5.S1-WALKTHROUGH.md
docs/learning/P5.S2-WALKTHROUGH.md
```

Repository entry points are README.md, AGENTS.md and ROADMAP.md. docs/ contains IN-SCOPE-OUT-OF-SCOPE.md, ROADMAP-REVIEW.md, LEARNING-SUPPORT-REQUIREMENTS.md, FLOWS-COMPARED.md, TEST-MATRIX.md and JWT-CLAIM-CONTRACT.md; docs/roadmap/ retains the five completed phase specifications.

## Architecture and lifecycle

The launcher excludes Boot’s default security/user/proxy auto-configuration. Exact mode validation defaults to none; conditional Basic/JWT configurations explicitly enable security and register the servlet proxy. None creates no security proxy/chain or identity/encoder infrastructure despite security libraries being packaged. permitAll is not used as a replacement for absence.

Basic and local JWT login explicitly import the shared plain identity configuration. JWT uses built-in BearerTokenAuthenticationFilter and JwtAuthenticationProvider with StrictJwtDecoder decorating NimbusJwtDecoder; Nimbus performs parsing/verification, the strict decorator preserves original JSON claim-type requirements. A separate local credential manager and NimbusJwtEncoder issue RS256 tokens. No custom bearer authentication filter, OAuth2 grant flow, external IdP or database exists.

TraceConfiguration registers the outer REQUEST filter before the security proxy and installs the MVC interceptor. Active lists are request-local; the sink retains bounded immutable completed snapshots. ERROR-dispatch observations are separate test probes. Local private keys belong in ignored /.local-keys, never src/main/resources or version control. No application resource configuration/private-key resource is required for the default none mode.
