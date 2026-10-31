# Project scope — Spring Security Learning

## Purpose and completion boundary

Build a compact, runnable, evidence-backed course explaining how HTTP requests move through Spring MVC and Spring Security. Learners must be able to connect observed behavior to application code and framework responsibilities, explain failures, and compare plain MVC, HTTP Basic, and RS256 JWT validation.

This scope covers the complete existing roadmap, **P1.S1 through P5.S2**, including the final learning package and verification. It is not authorization to implement later steps. Implement one numbered step per request, then stop. As of 2026-10-31, P1 and P2 are implemented and verified; P3.S1 onward remains planned.

[ROADMAP.md](../ROADMAP.md) controls sequence and completion status. Phase documents specify acceptance criteria. [LEARNING-SUPPORT-REQUIREMENTS.md](LEARNING-SUPPORT-REQUIREMENTS.md) defines teaching deliverables. [ROADMAP-REVIEW.md](ROADMAP-REVIEW.md) records recommended clarifications separately from completed work.

## In scope: architecture and behavior

- Java 21 compilation target, Spring Boot 3.x, Spring MVC, Maven, embedded Tomcat, and Spring Security 6.x when introduced. Record the actual runtime used; compilation for Java 21 does not prove execution on it.
- One application with mutually exclusive `none`, `basic`, and `jwt` modes once mode selection is introduced. Separate application contexts/configurations for mode-specific HTTP tests. Currently the single property `app.security-mode` accepts none/basic, defaults to none, and rejects jwt until P3.S2.
- `none`: genuinely no invocation of Spring Security's servlet filter infrastructure. Initially security dependencies are absent; since P2.S1, explicit conditional bean/servlet registration isolation preserves this behavior; security libraries remain present. `permitAll()` alone is insufficient.
- Three shared GET endpoints: `/api/public`, `/api/user`, `/api/admin`, with deterministic JSON. In the completed secured modes, public is permitted, user requires authentication, and admin requires ADMIN. P2.S2 implements credentials, and P2.S3 now enforces ADMIN on admin access. In plain mode, all three are accessible without credentials.
- Basic authentication with in-memory users, BCrypt password checking, and explicit authentication/authorization behavior. No persistent user management.
- A local POST `/auth/login` demonstration in JWT mode, introduced only at P3.S3. It checks credentials and issues a signed token; it does not implement an OAuth2 grant.
- JWT validation through Spring Security OAuth2 Resource Server, `BearerTokenAuthenticationFilter`, `JwtAuthenticationProvider`, and Nimbus-backed encoding/decoding. RS256 only; private-key signing and public-key verification remain distinct.
- Explicit issuer, audience, time, algorithm, and application-required claim validation; trusted role-to-authority conversion; positive and negative test evidence.
- Stateless secured API behavior, context population during processing, cleanup afterward, authentication challenges, and the distinction between 401 and 403.
- Local development key handling and test-generated keys. Private material is excluded from version control; passwords, complete bearer tokens, and private keys never appear in traces.

## In scope: complete phase deliverables

| Step | Included implementation or learning outcome | Status |
|---|---|---|
| P1.S1 | Boot/Maven bootstrap, three REST endpoints, real HTTP assertions, Surefire/Failsafe configuration, entry-point/MVC/JSON explanations | Implemented |
| P1.S2 | Explicit servlet filter and MVC interceptor registration, correlated bounded event sink, successful-request ordering and request isolation | Implemented |
| P1.S3 | Unmapped URL and thrown-controller-exception tests; observed callback/dispatch behavior; complete P1 teaching material | Implemented |
| P2.S1 | Security chain, genuine plain-mode isolation, stateless configuration, public/protected behavior, servlet/security-chain explanation | Implemented |
| P2.S2 | In-memory USER/ADMIN identities, BCrypt, Basic authentication, valid/invalid credentials, principal/context evidence | Implemented |
| P2.S3 | Admin authorization, USER denial, 401 challenge versus 403, sanitized security flow, complete P2 walkthroughs | Implemented |
| P3.S1 | Local RSA foundation, JWT header/claims/signature anatomy, RS256 signing/verification tests | Planned |
| P3.S2 | Resource Server mode using test-issued tokens, decoder/validators, initial role conversion, valid and invalid token HTTP tests | Planned |
| P3.S3 | Local credential-to-token login and end-to-end use of issued tokens; bad credentials and safe token contents | Planned |
| P4.S1 | Final role conversion and authorization contract, anonymous/missing-token/insufficient-authority cases, statelessness | Planned |
| P4.S2 | Tampering, wrong key/algorithm, malformed/expired/future tokens, issuer/audience, missing claims, malformed roles, deterministic time | Planned |
| P4.S3 | Actual security-filter ordering, correlated allowed/denied traces, authentication/authorization failure diagnostics, no secret leakage | Planned |
| P5.S1 | Equivalent real HTTP requests across isolated modes; reproducible success/denial traces; three sequence diagrams and comparative tables | Planned |
| P5.S2 | Complete code/documentation audit, `docs/learning/COMPLETE-GUIDE.md`, test cross-references, review questions/exercises, secret review, final `mvn verify`, accurate README/roadmap updates | Planned |

P3 introduces the minimal role conversion needed for valid token tests; P4 finalizes its edge cases and authorization evidence. P5 consolidates and audits existing behavior rather than introducing another authentication mechanism or product feature.

## In scope: verification and learning artifacts

Use JUnit 5, `@SpringBootTest(RANDOM_PORT)`, RestAssured, actual embedded Tomcat, and Maven Failsafe for HTTP integration tests. Unit tests supplement those tests when they check meaningful behavior. Run `mvn verify` for implementation steps and report executed test names, results, and precise blockers. Do not substitute MockMvc-only coverage or inferred success for real HTTP evidence.

Every step has a walkthrough explaining implemented classes, methods, annotations, meaningful code blocks, dependency injection, framework-owned calls, request/response sequencing, relevant failures, and limitations. Include Mermaid diagrams, actual sanitized correlated traces when instrumentation exists, test-to-code mappings, at least three review questions, and two safe exercises. Test traceability can be a compact table rather than a repeated standalone integration-test tutorial.

Maintain [FLOWS-COMPARED.md](FLOWS-COMPARED.md) as the shared flow comparison and [TEST-MATRIX.md](TEST-MATRIX.md) as the tested-versus-planned behavior index. Per-step evidence stays in walkthroughs; the final complete guide links the course together. Clearly distinguish observed calls/events from framework explanations and historical verification from current source references.

## Out of scope through P5.S2

| Area | Excluded work |
|---|---|
| OAuth2 identity platform | Authorization server, authorization-code/client-credentials or other grants, redirect login, consent, external identity providers, OIDC discovery integration |
| Persistent application data | Database, JPA, migrations, persistent user/token repositories, registration, profile management, password reset |
| Additional token systems | Refresh-token flows, revocation/blacklists, introspection services, alternative signing algorithms, production key rotation or remote JWKS distribution |
| Custom security engine | Custom bearer authentication filter, replacement JWT parser/signature implementation, controller/interceptor-based substitutes for Spring Security authentication |
| Frontend/browser application | UI, templates, frontend framework, browser login/session product, CORS feature development |
| Deployment platform | Docker, Kubernetes, cloud deployment, production TLS/certificate infrastructure, secrets-manager integration |
| Distributed architecture | Microservices, gateways, queues, caches, distributed tracing infrastructure, durable audit storage |
| Additional runtime models | WebFlux/reactive stack, executable async/streaming/WebSocket lifecycle demonstrations; explain async limitations without implementing them |
| Product hardening beyond the course | MFA, account lockout/rate-limiting systems, multi-tenancy, production monitoring, performance/load benchmarks |
| Unplanned teaching expansion | New phases, alternative auth mechanisms, broad framework rewrites, unrelated service/repository abstractions |

Exclusion from implementation does not forbid a short explanation of a limitation. For example, explain TLS needs, browser-related CSRF risks, token expiry limitations, and async dispatch caveats without building those systems. CSRF disablement is a bounded API-demo choice, not proof that stateless Basic authentication is inherently CSRF-safe; see [Spring Security's CSRF guidance](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html).

## Definition of done for the entire project

All fourteen numbered steps meet their acceptance criteria; isolated mode suites and the comparison matrix pass real HTTP verification; allowed and denied paths are explainable from observed evidence; all meaningful application code is covered by current teaching material; the final complete guide exists; and no excluded feature has been added implicitly. Final handoff records runtime versions, exact results, remaining limitations, and secret-handling checks. Scope changes require an explicit user request and a corresponding roadmap update, not speculative implementation.
