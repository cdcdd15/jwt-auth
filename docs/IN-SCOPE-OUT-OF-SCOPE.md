# Project scope through J3.S3

The objective is to learn Spring HTTP and security internals from a small runnable Java 17 JWT application and executed evidence. [ROADMAP](../ROADMAP.md) defines nine migration steps; one step is authorized per request. All nine steps are complete. J3 replaced documentation, assembled the complete source guide and performed the final audit. Documentation cleanup does not authorize new product features.

## In scope

| Area | Contract |
|---|---|
| Runtime/build | JDK 17 only, Spring Boot 3.x, MVC, embedded Tomcat, Maven compiler release 17 and Enforcer `[17,18)` |
| HTTP | GET public/user/admin JSON and POST `/auth/login`; successful messages remain public/user/admin |
| Credential authentication | Two local in-memory demonstration users, BCrypt, DaoAuthenticationProvider and a dedicated login ProviderManager |
| Issuance | JSON login signs trusted identity/roles using NimbusJwtEncoder, local RSA private key, RS256, 300-second lifetime, no-store response |
| Bearer authentication | Built-in BearerTokenAuthenticationFilter, JwtAuthenticationProvider, trusted local public key and Nimbus decoder |
| Validation | Required claims, fixed issuer/audience, 60-second skew, original JSON types and normalized checks; fail-closed keys |
| Authorization | Public and POST login permitted through security, user authenticated, admin requires ROLE_ADMIN, internal ERROR rendering permitted |
| Lifecycle evidence | Correlated bounded immutable REQUEST snapshots, real filter entry observations, separate test-only ERROR probes, context cleanup and overlapping identity isolation |
| Verification | Real HTTP RANDOM_PORT RestAssured/Failsafe; focused unit/context tests; deterministic clock and generated temporary keys |
| Learning | Per-step explanations, Mermaid sequences, actual sanitized traces, claim contract, test map and source references |
| Final delivery | J3.S2 has delivered the [production source/guide audit](learning/COMPLETE-GUIDE.md); [J3.S3](learning/J3.S3-WALKTHROUGH.md) completed clean JDK 17 verification and repository/history/log/package checks with recorded limitations |

## Out of scope

| Area | Excluded implementation |
|---|---|
| Other authentication modes | No selector, unsecured mode, HTTP Basic chain or cross-mode comparison scaffolding |
| Identity platform | OAuth2 authorization server/grants, OIDC discovery, external IdP, redirect/consent login |
| Persistence | Database, JPA, migrations, registration, password reset or persistent user/token storage |
| Token products | Refresh/revocation/blacklist services, introspection, remote JWKS, production rotation, alternative signing algorithms |
| Custom security engine | Custom bearer filter, replacement parser/signature engine, authentication performed by interceptors |
| UI/deployment | Frontend, Docker, Kubernetes, cloud/TLS infrastructure or secrets-manager integration |
| Other runtime models | Reactive/WebFlux, async/streaming/WebSocket experiments or distributed tracing |
| Product expansion | MFA, lockout/rate limiting, multi-tenancy, load benchmarking or additional service/repository layers |

Briefly explain limitations without implementing these systems. BCrypt is the existing local password-checking primitive, not a JWT signature algorithm. Stateless security does not prove universal browser CSRF safety or prevent arbitrary application code from creating a session. CSRF disablement is bounded to this non-browser demonstration.

## Definition of done

All nine roadmap steps must meet their acceptance criteria on JDK 17. A learner must be able to reconstruct credential authentication, bearer validation, authorization and MVC processing from current source and passing tests. J3.S3 alone closes the final audit. No feature beyond that step is implied. Secrets never belong in claims, traces or committed key files. See [claim contract](JWT-CLAIM-CONTRACT.md), [test matrix](TEST-MATRIX.md) and [learning requirements](LEARNING-SUPPORT-REQUIREMENTS.md).
