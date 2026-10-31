# Master Roadmap

Scope: [in scope / out of scope](docs/IN-SCOPE-OUT-OF-SCOPE.md), covering P1.S1 through P5.S2. Recommended clarifications are recorded in [the roadmap review](docs/ROADMAP-REVIEW.md); they do not authorize future implementation.

## Fixed design decisions
- Java 21; Spring Boot 3.x; Maven; embedded Tomcat; Spring MVC; Spring Security 6.x.
- One application, three mutually exclusive security modes: `none`, `basic`, `jwt`.
- JWT: Spring Security OAuth2 Resource Server + Nimbus, RS256 only; RSA private key signs, public key verifies.
- Local `/auth/login` is a demonstration credential-to-token endpoint, **not** an OAuth2 grant endpoint.
- In-memory users only; no DB, OAuth2 Authorization Server, browser OAuth2 login, Docker, or frontend.
- Integration tests: JUnit 5, SpringBootTest RANDOM_PORT, RestAssured, Maven Failsafe.

## Execution plan
- [x] P1 — Plain Spring MVC. See `docs/roadmap/P1-PLAIN-MVC.md`.
  - [x] P1.S1 — Bootstrap and endpoints; verified 2026-10-31: 3 HTTP tests passed (JDK 24, release 21). [Walkthrough](docs/learning/P1.S1-WALKTHROUGH.md).
  - [x] P1.S2 — Servlet filter and MVC interceptor; verified 2026-10-31: 7 HTTP cases + 2 unit tests passed (JDK 24, release 21). [Walkthrough](docs/learning/P1.S2-WALKTHROUGH.md).
  - [x] P1.S3 — Error paths and P1 teaching material; verified 2026-10-31: 9 HTTP cases + 2 unit tests passed (JDK 24, release 21). [Walkthrough](docs/learning/P1.S3-WALKTHROUGH.md).
- [ ] P2 — HTTP Basic: P2.S1, P2.S2, P2.S3. See `docs/roadmap/P2-BASIC-SECURITY.md`.
- [ ] P3 — RS256 foundation and issuance: P3.S1, P3.S2, P3.S3. See `docs/roadmap/P3-JWT-RS256.md`.
- [ ] P4 — JWT integration and edge cases: P4.S1, P4.S2, P4.S3. See `docs/roadmap/P4-JWT-VALIDATION.md`.
- [ ] P5 — Comparison and learning package: P5.S1, P5.S2. See `docs/roadmap/P5-COMPARISON.md`.

## Mandatory step exit gate
1. Implementation meets step scope and avoids future steps.
2. Real HTTP integration tests cover the step’s specified behavior; meaningful unit tests supplement them. Negative cases follow their assigned steps rather than being implemented early.
3. `mvn verify` succeeds (or a specific environment blocker is recorded).
4. Step-specific learning material is written and linked to actual code/tests.
5. Relevant roadmap checkbox and traceability notes are updated.

## Recommended execution
One Codex task per numbered step; review the evidence and learning notes between steps. Stop at the requested step. Deliver its walkthrough immediately; phase-end documentation work audits and consolidates existing walkthroughs.
