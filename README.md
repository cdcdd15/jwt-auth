# Spring Security Learning

A staged Java 21 / Spring Boot 3.x Maven learning project comparing:
1. Plain Spring MVC without Spring Security.
2. Spring Security HTTP Basic with in-memory users.
3. Spring Security OAuth2 Resource Server JWT bearer validation using Nimbus and RS256, with a simple local login issuer (not an OAuth2 Authorization Server).

**Status:** P1.S1 implemented and verified. Plain MVC serves three unauthenticated JSON endpoints; Spring Security is absent. P1.S2 and later steps remain unimplemented. See [the P1.S1 walkthrough](docs/learning/P1.S1-WALKTHROUGH.md).

## Start here
Read `AGENTS.md`, `ROADMAP.md`, `docs/LEARNING-SUPPORT-REQUIREMENTS.md`, `docs/PROJECT-STRUCTURE.md`, and the current phase document in `docs/roadmap/`. Implement only one numbered step per Codex execution, run its checks, and update its documentation before proceeding.

## Constraints
No OAuth2 authorization/grant flows, external identity provider, database, Docker, frontend, or custom JWT authentication filter. RS256 is mandatory. Use actual embedded Tomcat HTTP integration tests (`@SpringBootTest(RANDOM_PORT)` + RestAssured). Separate runtime modes: `none`, `basic`, `jwt`.

## Build
Run `mvn verify` (Java 21 compilation target). Maven Failsafe executes `*IT` integration tests; Surefire executes `*Test` unit tests. The P1.S1 verification passed all three real HTTP tests on the available JDK 24; execution on JDK 21 remains unverified.

Run locally with `mvn spring-boot:run` or `java -jar target/jwt-auth-0.0.1-SNAPSHOT.jar` after packaging. Default port: 8080. GET `/api/public`, `/api/user`, and `/api/admin` return `{"message":"public"}`, `{"message":"user"}`, and `{"message":"admin"}` respectively, all with HTTP 200 and no credentials. No mode-selection property is needed or implemented yet.
