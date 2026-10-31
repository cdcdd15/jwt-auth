# P2 — Spring Security with HTTP Basic

**Objective:** introduce Spring Security infrastructure while retaining P1 request-flow visibility.

## P2.S1 — Security chain and mode isolation

- [x] Implemented and verified 2026-10-31: `mvn verify` passed 15 integration + 9 unit cases, no failures/errors/skips (JDK 24, release 21).
- One property: `app.security-mode=none|basic`, default none; unsupported/combined values fail configuration creation. JWT is not implemented yet.
- Explicit isolation: Boot default chain, proxy registration, and generated-user auto-configurations excluded; only conditional basic configuration enables web security and registers the servlet proxy.
- At the S1 checkpoint, the entry point was status-only. S2 below now replaces it with a Basic challenge entry point and in-memory identities; ADMIN policy remains S3.
- `BasicSecurityIT`: real proxy invoked on public 200 and protected 401; denied requests have only outer filter events. `NoneSecurityIT`: three 200 responses with ignored invalid header; no chain/proxy bean or servlet registration. All P1 suites run explicitly in none.
- [Detailed walkthrough and actual chain/trace evidence](../learning/P2.S1-WALKTHROUGH.md). Tracing now precedes the security proxy.

Implemented design: Spring Security starter, explicit `SecurityFilterChain` for `basic`, and `none` mode with no security chain/proxy bean or servlet registration. Registration isolation is the selected strategy; no separate build profile is needed. Set stateless session policy, disable form login, disable CSRF only for this stateless API demo, permit `/api/public`, require auth elsewhere. Test public 200, unauthenticated protected 401 and verify `none` truly lacks a security filter invocation.
**Learning:** DelegatingFilterProxy, FilterChainProxy, chain selection, authorization, filter registration vs permitAll.

## P2.S2 — In-memory identity and Basic authentication

- [x] Implemented and verified 2026-10-31. `mvn verify`: 21 HTTP integration + 9 unit cases, zero failures/errors/skips (JDK 24, release 21).
- Conditional basic configuration provides BCrypt, in-memory users, explicit DaoAuthenticationProvider, and shared BasicAuthenticationEntryPoint. None contains no identity/encoder beans or security proxy/chain.
- `BasicSecurityIT`: both valid identities/authorities and erased credentials observed in MVC; missing credentials, bad password, unknown user, malformed Basic header, and public-route invalid credentials return 401 with the configured challenge. Anonymous public remains 200.
- The standard entry point's sendError is allowed to render through Boot ERROR dispatch without re-denial. Initial application-handler rejection and error rendering are distinguished; S3 still owns detailed security/error diagnostics and role policy.
- [Line-level walkthrough and actual sanitized evidence](../learning/P2.S2-WALKTHROUGH.md). No production principal endpoint or ADMIN access rule added.

Add users `user/user123` (USER), `admin/admin123` (ADMIN), BCrypt PasswordEncoder, InMemoryUserDetailsManager, AuthenticationManager/DaoAuthenticationProvider as needed. Enable HTTP Basic and replace S1's explicit status-only entry point with the intended Basic authentication entry point, so missing/invalid protected credentials return 401 with `WWW-Authenticate`. Update S1's intermediate assertions that currently require no challenge and no BasicAuthenticationFilter; preserve the actual proxy-invocation and none-isolation checks. Test valid and invalid credentials and security context principal using test-only observation rather than adding a production identity endpoint. Record invalid Basic credentials on the public route separately from public authorization without credentials. ADMIN role enforcement remains S3; do not implement it early. Demo credentials are development-only and must not appear in logs.
**Acceptance:** real HTTP cases cover public access, valid/invalid Basic credentials, challenge headers, and principal evidence; all none/P1 suites still pass. Write `docs/learning/P2.S2-WALKTHROUGH.md` in this step.
**Learning:** BasicAuthenticationFilter, Authentication, AuthenticationManager, Provider, UserDetailsService, PasswordEncoder, SecurityContext.

## P2.S3 — Authorization, failures and learning docs
`/api/admin` requires ADMIN; `/api/user` requires authenticated. Test USER 403 on admin, ADMIN 200, missing/incorrect Basic 401 and challenge header. Verify authenticated-then-anonymous and concurrent identity isolation without assuming a particular Tomcat worker is reused. Distinguish no session dependency from the absence of all possible servlet sessions. Instrument sanitized flow and explain exception translation and access denied vs authentication entry point. Audit the P2.S1–S3 walkthroughs delivered with their respective steps; update FLOWS-COMPARED and TEST-MATRIX.
Record allowed and denied requests with fixed event names, correlation IDs, and no credentials. The outer tracing filter already precedes security; extend evidence rather than re-registering it. Cover ERROR-dispatch policy explicitly so error handling does not accidentally replace the original response with a second authorization failure.
**Acceptance:** P1 + P2 integration suites pass, `mvn verify`, modes remain isolated.

## Exit criteria
Clear evidence for authentication vs authorization, actual filter ordering, 401 vs 403, and how Spring Security wraps MVC.
