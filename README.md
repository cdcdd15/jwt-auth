# Spring Security Learning

A staged Java 21 / Spring Boot 3.x Maven learning project comparing:
1. Plain Spring MVC without Spring Security.
2. Spring Security HTTP Basic with in-memory users.
3. Spring Security OAuth2 Resource Server JWT bearer validation using Nimbus and RS256, with a simple local login issuer (not an OAuth2 Authorization Server).

**Status:** P1–P5 complete. Final clean verification on **Java 21.0.8** passed all 153 HTTP integration cases and 29 unit cases. Start with the [complete learning guide](docs/learning/COMPLETE-GUIDE.md), including all fourteen walkthroughs, current source audit, questions and exercises. [Final audit](docs/learning/P5.S2-WALKTHROUGH.md) records exact commands, resolved versions, executed tests and secret-handling checks. Modes remain `none`, `basic`, and `jwt`; none has no security proxy/chain.

## Start here
The planned implementation is complete. Read [COMPLETE-GUIDE](docs/learning/COMPLETE-GUIDE.md) in course order or use [the comparison diagrams and matrix](docs/learning/P5.S1-WALKTHROUGH.md) as a reference. Any further implementation requires a new scoped request; the working agreement and existing phase exit gates remain applicable.

Project boundaries through P5.S2: [in scope / out of scope](docs/IN-SCOPE-OUT-OF-SCOPE.md). See [roadmap review recommendations](docs/ROADMAP-REVIEW.md) for clarifications before later steps.

## Constraints
No OAuth2 authorization/grant flows, external identity provider, database, Docker, frontend, or custom JWT authentication filter. RS256 is mandatory. Use actual embedded Tomcat HTTP integration tests (`@SpringBootTest(RANDOM_PORT)` + RestAssured). Separate runtime modes: `none`, `basic`, `jwt`.

## Build
Run `mvn verify` using Java21. Maven Failsafe executes `*IT` integration tests; Surefire executes `*Test` unit tests. Final verification used **`mvn clean verify`**, Maven 3.9.12 and JetBrains Java21.0.8: **153 HTTP + 29 unit cases**, zero failures/errors/skips, 29.403 seconds. The earlier JDK24/release21 limitation is closed by this actual Java21 run.

On this machine Java21 is bundled with IntelliJ rather than registered as the default JDK. Reproduce the verified command without changing global settings:

```bash
env JAVA_HOME='/Applications/IntelliJ IDEA CE.app/Contents/jbr/Contents/Home' \
  PATH="/Applications/IntelliJ IDEA CE.app/Contents/jbr/Contents/Home/bin:$PATH" \
  mvn clean verify
```

On another machine select its installed Java21 JDK and confirm `mvn -version` first. Resolved runtime: Boot3.5.7, Framework6.2.12, Security6.5.6, Tomcat10.1.48 and Nimbus9.37.4. [TEST-MATRIX](docs/TEST-MATRIX.md) indexes current coverage; historical walkthrough counts remain checkpoint evidence.

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

`/.local-keys/` is ignored. These commands are optional setup instructions; verification generates its own temporary keys using Java. POST `/auth/login` with JSON `username` and `password` using the development accounts above. Success returns JSON fields `accessToken`, `tokenType` (`Bearer`), and `expiresIn` (300 seconds), with `Cache-Control: no-store`. Send the returned value as `Authorization: Bearer <accessToken>` on a subsequent API request; avoid logging it. Wrong/unknown credentials receive the same 401 JSON; invalid request bodies receive 400. Login creates no session and is absent in none/basic. Basic headers cannot authenticate jwt mode. This is no OAuth2 grant and has no refresh endpoint. JWT role denial and missing/empty-role behavior are verified in P4.S1; the roadmap’s malformed-token/claim/time-boundary suite is now verified in P4.S2. JWT failure diagnostics and context isolation are now verified in P4.S3.
