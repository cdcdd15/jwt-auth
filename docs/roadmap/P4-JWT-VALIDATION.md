# P4 — Complete JWT Resource Server security behavior

**Objective:** solidify production-relevant validation behavior without implementing a custom bearer filter.

## P4.S1 — Authentication conversion and authorization
Finalize roles-to-GrantedAuthority mapping (`roles: ["USER"]` → `ROLE_USER`) only after successful signature/claims validation. Test user endpoint, admin endpoint, anonymous requests, missing bearer header and valid token without sufficient role. Ensure statelessness and no server session dependency.
**Learning:** JwtAuthenticationConverter, SecurityContextHolder, AuthorizationFilter, 401 vs 403.

## P4.S2 — Negative validation suite
Test tampered JWT, unknown RSA signing key, wrong algorithm (reject e.g. HS256), malformed token, expired token, future nbf, wrong issuer, wrong audience, missing mandatory claims, malformed roles claim. Use deterministic time handling; verify fail-closed behavior and error status. Never trust `alg` header without server-side algorithm allowlist.
**Learning:** JWS signature, JWT claims validators, algorithm confusion prevention, error paths.

## P4.S3 — Filter chain and error diagnostics
Add sanitized structured tracing across JWT request path; identify actual Spring Security filter order in running application, including BearerTokenAuthenticationFilter, ExceptionTranslationFilter, AuthorizationFilter, SecurityContext management and MVC handoff. Test trace correlation and no secret leakage. Explain authentication failure vs authorization failure handling. Produce P4.S1–S3 walkthroughs and update FLOWS-COMPARED / TEST-MATRIX.

## Exit criteria
Every JWT negative case has a test, documented expected behavior, and observed result. `mvn verify` passes in all modes. No custom JwtAuthenticationFilter.
