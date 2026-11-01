# Spring Security Learning

A staged Java 21 / Spring Boot 3.x Maven learning project comparing:
1. Plain Spring MVC without Spring Security.
2. Spring Security HTTP Basic with in-memory users.
3. Spring Security OAuth2 Resource Server JWT bearer validation using Nimbus and RS256, with a simple local login issuer (not an OAuth2 Authorization Server).

**Status:** P1–P3 implemented and verified. Modes are `none`, `basic`, and `jwt`. JWT mode now includes local JSON login and RS256 issuance, followed by Resource Server verification. See [P3.S3](docs/learning/P3.S3-WALKTHROUGH.md) and the [claim contract](docs/JWT-CLAIM-CONTRACT.md). None has no security proxy/chain; secured modes share BCrypt-backed in-memory identities. P4.S1 now verifies role conversion, 401/403 policy, and session-independent bearer requests. See [P4.S1](docs/learning/P4.S1-WALKTHROUGH.md); P4.S2 now verifies negative JWT inputs and exact skew boundaries, and preserves original JSON claim types through a strict decoder wrapper. See [P4.S2](docs/learning/P4.S2-WALKTHROUGH.md); P4.S3 remains planned.

## Start here
Read `AGENTS.md`, `ROADMAP.md`, `docs/LEARNING-SUPPORT-REQUIREMENTS.md`, `docs/PROJECT-STRUCTURE.md`, and the current phase document in `docs/roadmap/`. Implement only one numbered step per Codex execution, run its checks, and update its documentation before proceeding.

Project boundaries through P5.S2: [in scope / out of scope](docs/IN-SCOPE-OUT-OF-SCOPE.md). See [roadmap review recommendations](docs/ROADMAP-REVIEW.md) for clarifications before later steps.

## Constraints
No OAuth2 authorization/grant flows, external identity provider, database, Docker, frontend, or custom JWT authentication filter. RS256 is mandatory. Use actual embedded Tomcat HTTP integration tests (`@SpringBootTest(RANDOM_PORT)` + RestAssured). Separate runtime modes: `none`, `basic`, `jwt`.

## Build
Run `mvn verify` (Java 21 compilation target). Maven Failsafe executes `*IT` integration tests; Surefire executes `*Test` unit tests. The latest P4.S2 verification passed 106 real HTTP integration cases and twenty-nine unit cases on the available JDK 24; execution on JDK 21 remains unverified.

Run locally with `mvn spring-boot:run` or `java -jar target/jwt-auth-0.0.1-SNAPSHOT.jar` after packaging. Default port: 8080. GET `/api/public`, `/api/user`, and `/api/admin` return `{"message":"public"}`, `{"message":"user"}`, and `{"message":"admin"}` respectively, all with HTTP 200 and no credentials **in none mode**. Each response includes a server-generated `X-Correlation-ID`; the application retains the last 128 completed event snapshots for learning/tests. Default mode is `none`. Select the basic configuration with `mvn spring-boot:run -Dspring-boot.run.arguments=--app.security-mode=basic` or append `--app.security-mode=basic` to the packaged-jar command. Development-only accounts are `user/user123` (USER) and `admin/admin123` (ADMIN). Never reuse these outside the local demo or log their passwords/Authorization headers. `/api/user` requires authentication; `/api/admin` additionally requires ADMIN. Invalid supplied Basic credentials also return 401 on public access. Unknown/combined/blank modes fail startup. JWT mode requires matching local public/private keys; missing/invalid key material does not fall back to unsecured behavior.


JWT startup requires matching local RSA keys: X.509 public PEM and PKCS8 private PEM, at least 2048 bits. P3.S2’s public-only startup is superseded by the local issuer. For a local development pair (do not commit private material):

```bash
mkdir -p .local-keys
chmod 700 .local-keys
umask 077
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out .local-keys/private.pem
openssl pkey -in .local-keys/private.pem -pubout -out .local-keys/public.pem
mvn spring-boot:run -Dspring-boot.run.arguments="--app.security-mode=jwt --app.jwt.public-key-path=.local-keys/public.pem --app.jwt.private-key-path=.local-keys/private.pem"
```

`/.local-keys/` is ignored. These commands are optional setup instructions; verification generates its own temporary keys using Java. POST `/auth/login` with JSON `username` and `password` using the development accounts above. Success returns JSON fields `accessToken`, `tokenType` (`Bearer`), and `expiresIn` (300 seconds), with `Cache-Control: no-store`. Send the returned value as `Authorization: Bearer <accessToken>` on a subsequent API request; avoid logging it. Wrong/unknown credentials receive the same 401 JSON; invalid request bodies receive 400. Login creates no session and is absent in none/basic. Basic headers cannot authenticate jwt mode. This is no OAuth2 grant and has no refresh endpoint. JWT role denial and missing/empty-role behavior are verified in P4.S1; the roadmap’s malformed-token/claim/time-boundary suite is now verified in P4.S2. Detailed JWT diagnostics remain P4.S3.
