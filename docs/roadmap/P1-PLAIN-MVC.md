# P1 — Plain Spring MVC / Tomcat request lifecycle

**Objective:** observe servlet HTTP processing before Spring Security exists. No security dependency was present during P1.

**Historical phase:** P1 is complete. Since P2.S1, its suites run in explicit none mode with security libraries present but no security proxy/chain registration. Dependency-absence acceptance below describes the original P1 checkpoint, not the current jar.

## P1.S1 — Bootstrap and endpoints

- [x] Implemented and verified 2026-10-31. `mvn verify`: 3 tests, 0 failures/errors/skips. JDK 24 compiled with release 21; JDK 21 execution not verified.
- Evidence: `PlainMvcIT`, Failsafe reports in `target/failsafe-reports/`, resolved dependency tree and packaged jar contain no Spring Security.
- Learning: [P1.S1 walkthrough](../learning/P1.S1-WALKTHROUGH.md), including code references, lifecycle, diagrams, tests, and actual initialization logs.
- At the S1 checkpoint, correlated tracing was assigned to P1.S2 (now implemented below); error-path tests were assigned to P1.S3 (now implemented below).
Implement Java 21 Maven Spring Boot 3.x application, embedded Tomcat, `GET /api/public`, `/api/user`, `/api/admin` returning deterministic JSON. Configure Maven Surefire + Failsafe for `*Test` and `*IT`. Add RANDOM_PORT RestAssured integration tests for 200 and response bodies.
**Acceptance:** `mvn verify` passes; no Spring Security on classpath; application serves real HTTP.
**Learning:** entry point, component scanning, embedded Tomcat, controller annotations, DispatcherServlet mapping, JSON serialization.

## P1.S2 — Servlet filter and MVC interceptor

- [x] Implemented and verified 2026-10-31. Final `mvn verify`: 7 HTTP cases + 2 unit tests, zero failures/errors/skips; JDK 24, release 21.
- Evidence: `RequestTraceIT` verifies each endpoint’s six-event sequence and independent concurrent traces; `TraceEventSinkTest` verifies bounded retention, cleanup, and immutable snapshots.
- [Walkthrough with actual correlated events](../learning/P1.S2-WALKTHROUGH.md). REQUEST-only synchronous tracing is explicit; async/error lifecycle testing is not claimed.

Add `RequestTraceFilter` (OncePerRequestFilter), `MvcTraceInterceptor`, and test-observable event sink keyed by request/correlation ID. Register explicitly, avoid state leaks. Test successful request event ordering via real HTTP.
**Acceptance:** actual trace demonstrates filter entry → interceptor preHandle → controller → interceptor postHandle/afterCompletion → filter exit, with caveats for exceptions and async dispatch.
**Learning:** Servlet Filter vs HandlerInterceptor vs DispatcherServlet; chain.doFilter; handler mapping; response unwinding.

## P1.S3 — Error paths and P1 teaching material

- [x] Implemented and verified 2026-10-31. `mvn verify`: 9 HTTP integration cases + 2 unit tests; zero failures/errors/skips (JDK 24, release 21).
- `ErrorPathIT.unmappedUrlReturns404ThroughResourceHandlerAndErrorDispatch`: observed resource-handler REQUEST completion followed by Boot ERROR processing, returning 404 JSON.
- `ErrorPathIT.controllerExceptionReturns500AndNextRequestHasFreshTrace`: observed exception-bearing REQUEST completion, skipped REQUEST postHandle, successful ERROR processing, and a clean following request.
- [Walkthrough and actual correlated traces](../learning/P1.S3-WALKTHROUGH.md). Failing handler and dispatch probe are test-only; production remains three endpoints with REQUEST-only tracing.

Test unmapped URL and controller exception; document which hooks execute and in which order. Create `docs/learning/P1.S3-WALKTHROUGH.md`, audit and refresh the existing S1/S2 walkthroughs as needed, update `docs/FLOWS-COMPARED.md` and `docs/TEST-MATRIX.md`.
**Acceptance:** both happy and failure path tests pass, actual logs match documentation, `mvn verify` passes.

## Exit criteria
P1 exit criteria met: successful and 404/500 paths are explained with actual trace evidence; all P1 suites pass. No Basic/JWT code. Java 21 runtime execution remains unverified; compilation targets release 21. P2.S1 has since been completed; current next step is P2.S2.
