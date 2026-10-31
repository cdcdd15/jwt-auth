# Spring Security Learning

A staged Java 21 / Spring Boot 3.x Maven learning project comparing:
1. Plain Spring MVC without Spring Security.
2. Spring Security HTTP Basic with in-memory users.
3. Spring Security OAuth2 Resource Server JWT bearer validation using Nimbus and RS256, with a simple local login issuer (not an OAuth2 Authorization Server).

**Status:** P1 complete; P2.S1–S2 implemented and verified. `none` preserves the plain MVC path with no security chain/proxy registration, although Spring Security libraries are now present. `basic` supports BCrypt-backed in-memory USER/ADMIN authentication. Public access without credentials returns 200; protected requests require valid Basic credentials and otherwise return 401 with a Basic challenge. ADMIN-only access and 403 tests remain P2.S3. See [P2.S2 authentication](docs/learning/P2.S2-WALKTHROUGH.md), [P2.S1 mode isolation](docs/learning/P2.S1-WALKTHROUGH.md) and the [P1 error walkthrough](docs/learning/P1.S3-WALKTHROUGH.md).

## Start here
Read `AGENTS.md`, `ROADMAP.md`, `docs/LEARNING-SUPPORT-REQUIREMENTS.md`, `docs/PROJECT-STRUCTURE.md`, and the current phase document in `docs/roadmap/`. Implement only one numbered step per Codex execution, run its checks, and update its documentation before proceeding.

Project boundaries through P5.S2: [in scope / out of scope](docs/IN-SCOPE-OUT-OF-SCOPE.md). See [roadmap review recommendations](docs/ROADMAP-REVIEW.md) for clarifications before later steps.

## Constraints
No OAuth2 authorization/grant flows, external identity provider, database, Docker, frontend, or custom JWT authentication filter. RS256 is mandatory. Use actual embedded Tomcat HTTP integration tests (`@SpringBootTest(RANDOM_PORT)` + RestAssured). Separate runtime modes: `none`, `basic`, `jwt`.

## Build
Run `mvn verify` (Java 21 compilation target). Maven Failsafe executes `*IT` integration tests; Surefire executes `*Test` unit tests. The latest P2.S2 verification passed twenty-one real HTTP cases and nine unit cases on the available JDK 24; execution on JDK 21 remains unverified.

Run locally with `mvn spring-boot:run` or `java -jar target/jwt-auth-0.0.1-SNAPSHOT.jar` after packaging. Default port: 8080. GET `/api/public`, `/api/user`, and `/api/admin` return `{"message":"public"}`, `{"message":"user"}`, and `{"message":"admin"}` respectively, all with HTTP 200 and no credentials **in none mode**. Each response includes a server-generated `X-Correlation-ID`; the application retains the last 128 completed event snapshots for learning/tests. Default mode is `none`. Select the basic configuration with `mvn spring-boot:run -Dspring-boot.run.arguments=--app.security-mode=basic` or append `--app.security-mode=basic` to the packaged-jar command. Development-only accounts are `user/user123` (USER) and `admin/admin123` (ADMIN). Never reuse these outside the local demo or log their passwords/Authorization headers. Both protected endpoints currently require authentication; ADMIN enforcement comes in P2.S3. Invalid supplied Basic credentials also return 401 on public access. Unsupported modes, including `jwt` for now, fail startup.
