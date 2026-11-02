# Spring Security Learning

A Java 17 / Spring Boot 3.x learning application using unconditional Spring Security Resource Server JWT authentication with Nimbus and RS256, plus a local JSON credential-to-token issuer.

**Migration status:** J1.S1–J1.S2 complete on `jwt-only`. There is no security-mode selector or none/basic implementation. JWT security and local key loading are unconditional. See [the migration roadmap](ROADMAP.md) and [J1.S2](docs/learning/J1.S2-WALKTHROUGH.md). The old P1–P5 learning package remains historical until J3 replaces it.

## Start here
Implement one numbered migration step per request. J1.S3 is next and requires a separate request. Active requirements are Java17-only execution, built-in bearer authentication, strict validation and comprehensive per-step learning material. Historical three-mode preservation instructions are superseded by the migration roadmap.

## Constraints
No OAuth2 authorization/grant flows, external identity provider, database, Docker, frontend, or custom JWT authentication filter. RS256 is mandatory. Use actual embedded Tomcat HTTP integration tests (`@SpringBootTest(RANDOM_PORT)` + RestAssured). JWT is the sole security configuration; HTTP Basic cannot authenticate protected routes.

## Build — JDK 17 only

Use an installed **JDK17** for Maven, compilation, tests and application startup. Maven Enforcer runs at validate and accepts `[17,18)` only; JDK21/24 are rejected even when targeting release17. Surefire runs *Test, and Failsafe runs *IT over actual random-port Tomcat HTTP.

```bash
# Substitute your installed JDK17 home; verify Maven's reported Java version.
export JAVA_HOME=/absolute/path/to/jdk-17
export PATH="$JAVA_HOME/bin:$PATH"
mvn -version
mvn clean verify
```

The actual J1.S1 run used checksum-verified Eclipse Temurin17.0.21+9 temporarily extracted at `/private/tmp/jwt-auth-jdk17/jdk-17.0.21+9/Contents/Home`. This path is temporary and may disappear; it is not a permanent JDK installation. No global Java or IDE setting was changed. Select an installed JDK17 in your IDE for project SDK and Maven runner as well.

Verified 2026-11-02: Maven3.9.12, Java17.0.21, **94 HTTP + 23 unit cases**, zero failures/errors/skips, 20.310 s. Runtime dependencies remain Boot3.5.7, Framework6.2.12, Security6.5.6, Tomcat10.1.48 and Nimbus9.37.4. [J1.S1](docs/learning/J1.S1-WALKTHROUGH.md) records toolchain setup/runtime gating; [J1.S2](docs/learning/J1.S2-WALKTHROUGH.md) records the current test inventory and unconditional wiring.

Run locally with `mvn spring-boot:run` or `"$JAVA_HOME/bin/java" -jar target/jwt-auth-0.0.1-SNAPSHOT.jar` after packaging, supplying both key paths as below. Default port is8080. No mode property is required. GET `/api/public` allows anonymous access **through the security chain**; `/api/user` requires a valid bearer; `/api/admin` additionally requires ADMIN. Successful bodies remain `{"message":"public"}`, `{"message":"user"}`, and `{"message":"admin"}`. Responses carry a server-generated `X-Correlation-ID`; bounded completed traces support tests/learning.

Local demonstration accounts remain `user/user123` (USER) and `admin/admin123` (ADMIN) for **JSON POST login**, not Basic HTTP. They are not production credentials. Missing credentials or invalid bearer authentication on protected routes yield401; authenticated non-ADMIN admin access yields403. An old `app.security-mode` property has no consumer and cannot select an unsecured mode.

JWT startup requires matching local RSA keys: X.509 public PEM and PKCS8 private PEM, at least 2048 bits. P3.S2’s public-only startup is superseded by the local issuer. For a local development pair (do not commit private material):

```bash
mkdir -p .local-keys
chmod 700 .local-keys
umask 077
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out .local-keys/private.pem
openssl pkey -in .local-keys/private.pem -pubout -out .local-keys/public.pem
mvn spring-boot:run -Dspring-boot.run.arguments="--app.jwt.public-key-path=.local-keys/public.pem --app.jwt.private-key-path=.local-keys/private.pem"
```

`/.local-keys/` is ignored. These commands are optional setup instructions; verification generates its own temporary keys using Java. POST `/auth/login` with JSON `username` and `password` using the development accounts above. Success returns JSON fields `accessToken`, `tokenType` (`Bearer`), and `expiresIn` (300 seconds), with `Cache-Control: no-store`. Send the returned value as `Authorization: Bearer <accessToken>` on a subsequent API request; avoid logging it. Wrong/unknown credentials receive the same 401 JSON; invalid request bodies receive 400. Login creates no session. Basic headers cannot authenticate protected routes. This is no OAuth2 grant and has no refresh endpoint. JWT role denial and missing/empty-role behavior are verified in P4.S1; the roadmap’s malformed-token/claim/time-boundary suite is now verified in P4.S2. JWT failure diagnostics and context isolation are now verified in P4.S3.
