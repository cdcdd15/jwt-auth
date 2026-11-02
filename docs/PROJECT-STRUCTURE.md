# Implemented project structure

Java 17, Spring Boot MVC and unconditional Resource Server JWT security form the current application. [ROADMAP](../ROADMAP.md) owns progress; [learning index](learning/README.md) owns navigation. No selector or alternative security configuration exists.

## Production sources

- [SecurityLearningApplication.java](../src/main/java/com/example/securitylearning/SecurityLearningApplication.java) — Boot entry point, component scan and explicit proxy auto-registration exclusion.
- [api/AuthController.java](../src/main/java/com/example/securitylearning/api/AuthController.java) — JSON login binding, no-store response and local authentication failure handling.
- [api/DemoController.java](../src/main/java/com/example/securitylearning/api/DemoController.java) — Three deterministic message endpoints and controller trace markers.
- [config/InMemoryIdentityConfig.java](../src/main/java/com/example/securitylearning/config/InMemoryIdentityConfig.java) — BCrypt, in-memory users, Dao provider and dedicated login manager.
- [config/JwtDecoderConfig.java](../src/main/java/com/example/securitylearning/config/JwtDecoderConfig.java) — Local public key, UTC Clock, RS256 Nimbus and semantic/timestamp validators.
- [config/JwtIssuerConfig.java](../src/main/java/com/example/securitylearning/config/JwtIssuerConfig.java) — Matching private key validation and Nimbus encoder.
- [config/JwtSecurityConfig.java](../src/main/java/com/example/securitylearning/config/JwtSecurityConfig.java) — Converter, endpoint policy, standard bearer failure handlers and explicit servlet proxy.
- [security/JwtClaimContractValidator.java](../src/main/java/com/example/securitylearning/security/JwtClaimContractValidator.java) — Normalized required/type/audience contract.
- [security/JwtTokenService.java](../src/main/java/com/example/securitylearning/security/JwtTokenService.java) — Credential authentication, trusted claim building and signed token response.
- [security/RsaKeyGenerator.java](../src/main/java/com/example/securitylearning/security/RsaKeyGenerator.java) — On-demand development RSA generation, no automatic file export.
- [security/StrictJwtDecoder.java](../src/main/java/com/example/securitylearning/security/StrictJwtDecoder.java) — Original JSON type guard after trusted Nimbus validation.
- [tracing/MvcTraceInterceptor.java](../src/main/java/com/example/securitylearning/tracing/MvcTraceInterceptor.java) — REQUEST-only MVC callbacks.
- [tracing/RequestTraceFilter.java](../src/main/java/com/example/securitylearning/tracing/RequestTraceFilter.java) — Outer UUID and finally snapshot publication.
- [tracing/SecurityTraceFilter.java](../src/main/java/com/example/securitylearning/tracing/SecurityTraceFilter.java) — Context observation before authorization, no authentication decisions.
- [tracing/TraceConfiguration.java](../src/main/java/com/example/securitylearning/tracing/TraceConfiguration.java) — Sink/filter/interceptor registration.
- [tracing/TraceEventSink.java](../src/main/java/com/example/securitylearning/tracing/TraceEventSink.java) — Request-local active lists and 128 immutable completed snapshots.

## Tests and fixtures

All integration suites use actual random-port Tomcat HTTP and RestAssured. Configurations/probes differ by suite and use isolated contexts. Surefire owns *Test; Failsafe owns *IT. [TEST-MATRIX](TEST-MATRIX.md) records current counts and case ownership.

- [ErrorPathIT.java](../src/test/java/com/example/securitylearning/ErrorPathIT.java)
- [JwtAuthorizationIT.java](../src/test/java/com/example/securitylearning/JwtAuthorizationIT.java)
- [JwtDiagnosticsIT.java](../src/test/java/com/example/securitylearning/JwtDiagnosticsIT.java)
- [JwtLoginIT.java](../src/test/java/com/example/securitylearning/JwtLoginIT.java)
- [JwtNegativeValidationIT.java](../src/test/java/com/example/securitylearning/JwtNegativeValidationIT.java)
- [RequestTraceIT.java](../src/test/java/com/example/securitylearning/RequestTraceIT.java)
- [config/JwtDecoderConfigTest.java](../src/test/java/com/example/securitylearning/config/JwtDecoderConfigTest.java)
- [config/JwtIssuerConfigTest.java](../src/test/java/com/example/securitylearning/config/JwtIssuerConfigTest.java)
- [config/JwtStartupTest.java](../src/test/java/com/example/securitylearning/config/JwtStartupTest.java)
- [security/IssuerSecretHandlingTest.java](../src/test/java/com/example/securitylearning/security/IssuerSecretHandlingTest.java)
- [security/JwtClaimContractValidatorTest.java](../src/test/java/com/example/securitylearning/security/JwtClaimContractValidatorTest.java)
- [security/RsaJwtTest.java](../src/test/java/com/example/securitylearning/security/RsaJwtTest.java)
- [support/JwtKeyFixture.java](../src/test/java/com/example/securitylearning/support/JwtKeyFixture.java)
- [support/JwtTestTokens.java](../src/test/java/com/example/securitylearning/support/JwtTestTokens.java)
- [tracing/TraceEventSinkTest.java](../src/test/java/com/example/securitylearning/tracing/TraceEventSinkTest.java)

ThrowingController, principal/ERROR probes, filter decorators and generated temporary key fixtures exist only under src/test. They are not product endpoints/infrastructure and are not packaged as application classes.

## Wiring and request path

The launcher excludes only SecurityFilterAutoConfiguration because JwtSecurityConfig explicitly owns servlet DelegatingFilterProxy registration. It does not disable all security auto-configuration. InMemoryIdentityConfig is scanned and owns credential authentication. JwtIssuerConfig owns signing, JwtDecoderConfig owns verification, and JwtSecurityConfig owns bearer conversion/policy. Both key paths are required unconditionally.

TraceConfiguration registers outer REQUEST tracing at HIGHEST_PRECEDENCE+10, before the security proxy at -100 for REQUEST/ERROR. FilterChainProxy selects the configured chain. Built-in bearer authentication verifies and converts tokens before AuthorizationFilter applies policy. Allowed requests reach DispatcherServlet, mapping/adapter and controllers. Jackson writes JSON during response-body processing; interceptor completion and filter finally blocks unwind afterward. Boot ERROR rendering is allowed for internal ERROR dispatches, while a direct anonymous REQUEST to `/error` remains protected.

## Documentation ownership

- Root README: build/start/endpoint instructions. AGENTS: working agreement. ROADMAP: nine-step specification and progress.
- Scope, learning requirements and claim contract: current constraints and acceptance.
- TEST-MATRIX: current coverage. This file: actual inventory.
- learning/README: existing walkthroughs and explicitly pending steps.
- learning/JWT-FOUNDATIONS: retained MVC, credential, JWT, tracing and error explanations with current sources.
- learning/COMPLETE-GUIDE: complete current production source/block audit, request sequences, test mapping, interview questions and actual evidence.
- J walkthroughs: immutable execution evidence with superseded source/count notes where appropriate.

The original course documents are removed from this branch; recover them through master/Git history. No private keys belong in src/main/resources. Local development files belong under ignored /.local-keys; tests create and clean their own owner-only temporary files.

J3.S2 source/flow audit: [complete guide](learning/COMPLETE-GUIDE.md) and [walkthrough](learning/J3.S2-WALKTHROUGH.md). All 16 production files and21 HTTP test methods are covered; final repository audit remains J3.S3.
