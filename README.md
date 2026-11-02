# Spring Security Learning

A Java 17 / Spring Boot 3.x learning project, currently migrating to JWT-only. During J1.S1 the existing application still compares:
1. Plain Spring MVC without Spring Security.
2. Spring Security HTTP Basic with in-memory users.
3. Spring Security OAuth2 Resource Server JWT bearer validation using Nimbus and RS256, with a simple local login issuer (not an OAuth2 Authorization Server).

**Migration status:** J1.S1 complete on branch `jwt-only`: compiler release17 and a Java17-only Maven gate are verified. A clean build on Temurin17.0.21 passed 153 HTTP integration + 29 unit cases. None/basic/jwt behavior is unchanged; J1.S2 will remove the other modes. Read [the migration roadmap](ROADMAP.md) and [J1.S1’s walkthrough](docs/learning/J1.S1-WALKTHROUGH.md). The P1–P5 material remains historical until J3 replaces it.

## Start here
Implement one numbered migration step per request using [ROADMAP.md](ROADMAP.md). J1.S2 is next, requiring a separate request. The existing [complete guide](docs/learning/COMPLETE-GUIDE.md) explains the three-mode implementation; its original Java21 commands/results are historical. The current runtime/build requirement is Java17 only. Historical scope/structure/phase documents will be replaced at J3.S1; the migration roadmap controls conflicts.

## Constraints
No OAuth2 authorization/grant flows, external identity provider, database, Docker, frontend, or custom JWT authentication filter. RS256 is mandatory. Use actual embedded Tomcat HTTP integration tests (`@SpringBootTest(RANDOM_PORT)` + RestAssured). Separate runtime modes: `none`, `basic`, `jwt`.

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

Verified 2026-11-02: Maven3.9.12, Java17.0.21, **153 HTTP + 29 unit cases**, zero failures/errors/skips, 29.550 seconds. Runtime dependencies remain Boot3.5.7, Framework6.2.12, Security6.5.6, Tomcat10.1.48 and Nimbus9.37.4. [J1.S1](docs/learning/J1.S1-WALKTHROUGH.md) records exact commands, the unsupported-runtime checks and current test inventory.

Run locally with `mvn spring-boot:run` or `"$JAVA_HOME/bin/java" -jar target/jwt-auth-0.0.1-SNAPSHOT.jar` after packaging. Default port: 8080. GET `/api/public`, `/api/user`, and `/api/admin` return `{"message":"public"}`, `{"message":"user"}`, and `{"message":"admin"}` respectively, all with HTTP 200 and no credentials **in none mode**. Each response includes a server-generated `X-Correlation-ID`; the application retains the last 128 completed event snapshots for learning/tests. Default mode is `none`. Select the basic configuration with `mvn spring-boot:run -Dspring-boot.run.arguments=--app.security-mode=basic` or append `--app.security-mode=basic` to the packaged-jar command. Development-only accounts are `user/user123` (USER) and `admin/admin123` (ADMIN). Never reuse these outside the local demo or log their passwords/Authorization headers. `/api/user` requires authentication; `/api/admin` additionally requires ADMIN. Invalid supplied Basic credentials also return 401 on public access. Unknown/combined/blank modes fail startup. JWT mode requires matching local public/private keys; missing/invalid key material does not fall back to unsecured behavior.


JWT startup requires matching local RSA keys: X.509 public PEM and PKCS8 private PEM, at least 2048 bits. P3.S2’s public-only startup is superseded by the local issuer. For a local development pair (do not commit private material):

```bash
mkdir -p .local-keys
chmod 700 .local-keys
umask 077
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out .local-keys/private.pem
openssl pkey -in .local-keys/private.pem -pubout -out .local-keys/public.pem
mvn spring-boot:run -Dspring-boot.run.arguments="--app.security-mode=jwt --app.jwt.public-key-path=.local-keys/public.pem --app.jwt.private-key-path=.local-keys/private.pem"
```

`/.local-keys/` is ignored. These commands are optional setup instructions; verification generates its own temporary keys using Java. POST `/auth/login` with JSON `username` and `password` using the development accounts above. Success returns JSON fields `accessToken`, `tokenType` (`Bearer`), and `expiresIn` (300 seconds), with `Cache-Control: no-store`. Send the returned value as `Authorization: Bearer <accessToken>` on a subsequent API request; avoid logging it. Wrong/unknown credentials receive the same 401 JSON; invalid request bodies receive 400. Login creates no session and is absent in none/basic. Basic headers cannot authenticate jwt mode. This is no OAuth2 grant and has no refresh endpoint. JWT role denial and missing/empty-role behavior are verified in P4.S1; the roadmap’s malformed-token/claim/time-boundary suite is now verified in P4.S2. JWT failure diagnostics and context isolation are now verified in P4.S3.
