# Master Roadmap

## Fixed design decisions
- Java 21; Spring Boot 3.x; Maven; embedded Tomcat; Spring MVC; Spring Security 6.x.
- One application, three mutually exclusive security modes: `none`, `basic`, `jwt`.
- JWT: Spring Security OAuth2 Resource Server + Nimbus, RS256 only; RSA private key signs, public key verifies.
- Local `/auth/login` is a demonstration credential-to-token endpoint, **not** an OAuth2 grant endpoint.
- In-memory users only; no DB, OAuth2 Authorization Server, browser OAuth2 login, Docker, or frontend.
- Integration tests: JUnit 5, SpringBootTest RANDOM_PORT, RestAssured, Maven Failsafe.

## Execution plan
- [ ] P1 — Plain Spring MVC. See `docs/roadmap/P1-PLAIN-MVC.md`.
  - [x] P1.S1 — Bootstrap and endpoints; verified 2026-10-31: 3 HTTP tests passed (JDK 24, release 21). [Walkthrough](docs/learning/P1.S1-WALKTHROUGH.md).
  - [ ] P1.S2 — Servlet filter and MVC interceptor.
  - [ ] P1.S3 — Error paths and P1 teaching material.
- [ ] P2 — HTTP Basic: P2.S1, P2.S2, P2.S3. See `docs/roadmap/P2-BASIC-SECURITY.md`.
- [ ] P3 — RS256 foundation and issuance: P3.S1, P3.S2, P3.S3. See `docs/roadmap/P3-JWT-RS256.md`.
- [ ] P4 — JWT integration and edge cases: P4.S1, P4.S2, P4.S3. See `docs/roadmap/P4-JWT-VALIDATION.md`.
- [ ] P5 — Comparison and learning package: P5.S1, P5.S2. See `docs/roadmap/P5-COMPARISON.md`.

## Mandatory step exit gate
1. Implementation meets step scope and avoids future steps.
2. Automated unit/integration tests cover positive and negative behavior.
3. `mvn verify` succeeds (or a specific environment blocker is recorded).
4. Step-specific learning material is written and linked to actual code/tests.
5. Relevant roadmap checkbox and traceability notes are updated.

## Recommended execution
One Codex task per numbered step; review the evidence and learning notes between steps. Combine only when measured execution is fast and acceptance criteria remain independent.
