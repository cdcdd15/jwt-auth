# P4 — Complete JWT Resource Server security behavior

**Objective:** solidify production-relevant validation behavior without implementing a custom bearer filter.

## P4.S1 — Authentication conversion and authorization
Refine the existing P3 mapping and its tests rather than introducing a second authentication path. Finalize roles-to-GrantedAuthority mapping (`roles: ["USER"]` → `ROLE_USER`) only after successful signature/claims validation. Test user endpoint, admin endpoint, anonymous requests, missing bearer header and valid token without sufficient role. Ensure statelessness and no server session dependency.
**Learning:** JwtAuthenticationConverter, SecurityContextHolder, AuthorizationFilter, 401 vs 403.

## P4.S2 — Negative validation suite
Use the claim contract defined in P3; explicitly assert missing-versus-malformed claim outcomes and test time boundaries against the configured skew. Do not assume default validators enforce every application-required claim. Test tampered JWT, unknown RSA signing key, wrong algorithm (reject e.g. HS256), malformed token, expired token, future nbf, wrong issuer, wrong audience, missing mandatory claims, malformed roles claim. Use deterministic time handling; verify fail-closed behavior and error status. Never trust `alg` header without server-side algorithm allowlist.
**Learning:** JWS signature, JWT claims validators, algorithm confusion prevention, error paths.

## P4.S3 — Filter chain and error diagnostics
Add sanitized structured tracing across JWT request path; identify actual Spring Security filter order in running application, including BearerTokenAuthenticationFilter, ExceptionTranslationFilter, AuthorizationFilter, SecurityContext management and MVC handoff. Test trace correlation and no secret leakage, including authenticated-then-anonymous and concurrent identity isolation. Distinguish configured chain order from observed filter invocations; cover early token rejection before authorization/MVC and role denial after authentication. Explain authentication failure vs authorization failure handling. Audit the P4.S1–S3 walkthroughs delivered with their respective steps and update FLOWS-COMPARED / TEST-MATRIX.

## Exit criteria
Every JWT negative case has a test, documented expected behavior, and observed result. `mvn verify` passes in all modes. No custom JwtAuthenticationFilter.
