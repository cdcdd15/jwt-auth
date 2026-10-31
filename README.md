# Spring Security Learning

A staged Java 21 / Spring Boot 3.x Maven learning project comparing:
1. Plain Spring MVC without Spring Security.
2. Spring Security HTTP Basic with in-memory users.
3. Spring Security OAuth2 Resource Server JWT bearer validation using Nimbus and RS256, with a simple local login issuer (not an OAuth2 Authorization Server).

**Status:** P1/P2 and P3.S1–S2 implemented and verified. Modes are `none`, `basic`, and `jwt`. JWT mode validates RS256 bearer tokens with a local public key, required claims, issuer/audience, and timestamps; USER/user and ADMIN/admin HTTP cases pass. Token issuance exists only in tests—`/auth/login` and a runtime signer remain P3.S3. See [P3.S2](docs/learning/P3.S2-WALKTHROUGH.md), the [claim contract](docs/JWT-CLAIM-CONTRACT.md), and [Basic behavior](docs/learning/P2.S3-WALKTHROUGH.md). None has no security proxy/chain; Basic retains BCrypt-backed users and ADMIN authorization.

## Start here
Read `AGENTS.md`, `ROADMAP.md`, `docs/LEARNING-SUPPORT-REQUIREMENTS.md`, `docs/PROJECT-STRUCTURE.md`, and the current phase document in `docs/roadmap/`. Implement only one numbered step per Codex execution, run its checks, and update its documentation before proceeding.

Project boundaries through P5.S2: [in scope / out of scope](docs/IN-SCOPE-OUT-OF-SCOPE.md). See [roadmap review recommendations](docs/ROADMAP-REVIEW.md) for clarifications before later steps.

## Constraints
No OAuth2 authorization/grant flows, external identity provider, database, Docker, frontend, or custom JWT authentication filter. RS256 is mandatory. Use actual embedded Tomcat HTTP integration tests (`@SpringBootTest(RANDOM_PORT)` + RestAssured). Separate runtime modes: `none`, `basic`, `jwt`.

## Build
Run `mvn verify` (Java 21 compilation target). Maven Failsafe executes `*IT` integration tests; Surefire executes `*Test` unit tests. The latest P3.S2 verification passed thirty-five real HTTP integration cases and twenty-five unit cases on the available JDK 24; execution on JDK 21 remains unverified.

Run locally with `mvn spring-boot:run` or `java -jar target/jwt-auth-0.0.1-SNAPSHOT.jar` after packaging. Default port: 8080. GET `/api/public`, `/api/user`, and `/api/admin` return `{"message":"public"}`, `{"message":"user"}`, and `{"message":"admin"}` respectively, all with HTTP 200 and no credentials **in none mode**. Each response includes a server-generated `X-Correlation-ID`; the application retains the last 128 completed event snapshots for learning/tests. Default mode is `none`. Select the basic configuration with `mvn spring-boot:run -Dspring-boot.run.arguments=--app.security-mode=basic` or append `--app.security-mode=basic` to the packaged-jar command. Development-only accounts are `user/user123` (USER) and `admin/admin123` (ADMIN). Never reuse these outside the local demo or log their passwords/Authorization headers. `/api/user` requires authentication; `/api/admin` additionally requires ADMIN. Invalid supplied Basic credentials also return 401 on public access. Unknown/combined/blank modes fail startup. JWT mode requires a local public key; missing/invalid key material does not fall back to unsecured behavior.


JWT validation-only startup with an existing local RSA public PEM (at least 2048 bits):

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--app.security-mode=jwt --app.jwt.public-key-path=/absolute/path/to/public.pem"
```

Use `mvn verify` for the self-contained token fixtures and fixed-clock JWT HTTP tests. No private key is loaded by the runtime in P3.S2, and Basic credentials cannot authenticate jwt mode. P4's complete JWT edge-case/role-denial matrix remains planned.
