# P2 — Spring Security with HTTP Basic

**Objective:** introduce Spring Security infrastructure while retaining P1 request-flow visibility.

## P2.S1 — Security chain and mode isolation
Add Spring Security starter, explicit `SecurityFilterChain` for `basic`, and robust `none` mode that actually bypasses/disables the Spring Security servlet filter (or a build-profile strategy excluding security). Set stateless session policy, disable form login, disable CSRF only for this stateless API demo, permit `/api/public`, require auth elsewhere. Test public 200, unauthenticated protected 401 and verify `none` truly lacks a security filter invocation.
**Learning:** DelegatingFilterProxy, FilterChainProxy, chain selection, authorization, filter registration vs permitAll.

## P2.S2 — In-memory identity and Basic authentication
Add users `user/user123` (USER), `admin/admin123` (ADMIN), BCrypt PasswordEncoder, InMemoryUserDetailsManager, AuthenticationManager/DaoAuthenticationProvider as needed. Enable HTTP Basic. Test valid and invalid credentials and security context principal. Demo credentials are development-only and must not appear in logs.
**Learning:** BasicAuthenticationFilter, Authentication, AuthenticationManager, Provider, UserDetailsService, PasswordEncoder, SecurityContext.

## P2.S3 — Authorization, failures and learning docs
`/api/admin` requires ADMIN; `/api/user` requires authenticated. Test USER 403 on admin, ADMIN 200, missing/incorrect Basic 401 and challenge header. Instrument sanitized flow and explain exception translation and access denied vs authentication entry point. Produce P2.S1–S3 learning walkthroughs; update FLOWS-COMPARED and TEST-MATRIX.
**Acceptance:** P1 + P2 integration suites pass, `mvn verify`, modes remain isolated.

## Exit criteria
Clear evidence for authentication vs authorization, actual filter ordering, 401 vs 403, and how Spring Security wraps MVC.
