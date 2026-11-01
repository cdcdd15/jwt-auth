# P4 — Complete JWT Resource Server security behavior

**Objective:** solidify production-relevant validation behavior without implementing a custom bearer filter.

**Status:** P4 complete; final verification passed 114 HTTP + 29 unit cases on 2026-11-01. P5 remains planned. S2 adds a strict JwtDecoder decorator inside the existing Resource Server authentication path; no custom bearer filter.

## P4.S1 — Authentication conversion and authorization

- [x] Verified 2026-11-01: `mvn verify` passed 61 HTTP integration + 29 unit cases, zero failures/errors/skips, 15.272 seconds (JDK 24/release 21).
- `JwtValidationIT` adds 17 parameterized/test cases: five request-principal/authority checks, nine authorization cells, two missing-bearer cases, and one session-independence sequence.
- Actual request authorities match ROLE_USER/ROLE_ADMIN/ROLE_OTHER; missing/empty roles authenticate with no authorities. USER, absent/empty roles and OTHER fail admin with 403/Bearer insufficient_scope. ADMIN accesses both routes; anonymous protected requests get 401/Bearer.
- Existing conversion and policy were correct; production classes remain unchanged. Probe is test-only, explicitly imported, and excluded from ordinary component scanning.
- [Detailed source explanation, actual traces, and request sequence](../learning/P4.S1-WALKTHROUGH.md). At the S1 checkpoint, full malformed-claim/time-boundary and diagnostic suites were pending; S2/S3 below now provide them.

Refine the existing P3 mapping and its tests rather than introducing a second authentication path. Finalize roles-to-GrantedAuthority mapping (`roles: ["USER"]` → `ROLE_USER`) only after successful signature/claims validation. Test user endpoint, admin endpoint, anonymous requests, missing bearer header and valid token without sufficient role. Ensure statelessness and no server session dependency.
**Learning:** JwtAuthenticationConverter, SecurityContextHolder, AuthorizationFilter, 401 vs 403.

## P4.S2 — Negative validation suite

- [x] Verified 2026-11-01: `mvn verify` passed 106 HTTP integration + 29 unit cases, zero failures/errors/skips, 21.343 seconds (JDK 24/release 21).
- `JwtNegativeValidationIT` adds 45 real HTTP cases: 34 malformed/unacceptable inputs, six exact timestamp-skew boundaries, five permitted-representation controls.
- Initial tests exposed accepted numeric sub and null iat/nbf after library normalization. `StrictJwtDecoder` delegates verification/normalized validation to Nimbus first, then uses Nimbus to check original payload JSON types before returning the Jwt to the provider/converter.
- Invalid inputs return 401/Bearer invalid_token before MVC; exp NOW−60 and nbf NOW+60 pass, one second farther fails. Missing optional dates/roles and single-string audience remain accepted.
- [Current source commentary, actual traces, and case map](../learning/P4.S2-WALKTHROUGH.md). P4.S3 below now supplies those diagnostics.

Use the claim contract defined in P3; explicitly assert missing-versus-malformed claim outcomes and test time boundaries against the configured skew. Do not assume default validators enforce every application-required claim. Test tampered JWT, unknown RSA signing key, wrong algorithm (reject e.g. HS256), malformed token, expired token, future nbf, wrong issuer, wrong audience, missing mandatory claims, malformed roles claim. Use deterministic time handling; verify fail-closed behavior and error status. Never trust `alg` header without server-side algorithm allowlist.
**Learning:** JWS signature, JWT claims validators, algorithm confusion prevention, error paths.

## P4.S3 — Filter chain and error diagnostics

- [x] Verified 2026-11-01: `mvn verify` passed 114 HTTP integration + 29 unit cases, zero failures/errors/skips, 23.245 seconds (JDK 24/release 21).
- JWT handlers now record fixed REQUEST-only challenge/denied events and delegate unchanged HTTP responses to Spring’s Bearer handlers. Prior JWT rejection trace expectations are updated.
- `JwtDiagnosticsIT` adds eight real HTTP cases: five allowed/denied/early-rejection branches, sequential and concurrent identity isolation, and captured-output/sink checks including real JSON login.
- Test-only filter-chain decoration observes actual filter entries separately from configured order. A spy calls the real proxy and records empty worker-thread authentication before defensive cleanup. No custom bearer filter.
- P4 walkthroughs audited; current trace contract, source references and actual evidence are in [P4.S3](../learning/P4.S3-WALKTHROUGH.md). Older checkpoint traces are marked historical. None/basic regressions remain green.

Add sanitized structured tracing across JWT request path; identify actual Spring Security filter order in running application, including BearerTokenAuthenticationFilter, ExceptionTranslationFilter, AuthorizationFilter, SecurityContext management and MVC handoff. Test trace correlation and no secret leakage, including authenticated-then-anonymous and concurrent identity isolation. Distinguish configured chain order from observed filter invocations; cover early token rejection before authorization/MVC and role denial after authentication. Explain authentication failure vs authorization failure handling. Audit the P4.S1–S3 walkthroughs delivered with their respective steps and update FLOWS-COMPARED / TEST-MATRIX.

## Exit criteria
Every JWT negative case has a test, documented expected behavior, and observed result. `mvn verify` passes in all modes. No custom JwtAuthenticationFilter.
