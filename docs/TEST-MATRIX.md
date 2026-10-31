# HTTP Integration Test Matrix — P1/P2 HTTP and P3.S1 crypto verified; JWT HTTP/comparison planned

Current verified checkpoint: **P3.S1**, with 27 real HTTP integration cases and 13 unit cases passing on JDK 24 with release 21. None has no security registration/identity beans. Basic verifies USER/admin 403, ADMIN/admin 200, valid user access for both identities, anonymous public 200, missing/invalid credentials 401 with challenges, fixed security events, context cleanup, identity isolation, and preserved error rendering. JWT signing/verification now has 4 unit cases; JWT HTTP mode remains unimplemented. See [P2 HTTP evidence](learning/P2.S3-WALKTHROUGH.md) and [P3.S1 crypto evidence](learning/P3.S1-WALKTHROUGH.md).

## Target matrix for completed modes — not all cells executed

The table describes the completed project contract. Current coverage includes the three anonymous rows in none/basic, invalid-bearer requests in none, valid USER/ADMIN Basic access to user, and Basic challenge behavior. Basic admin role decisions are now verified in S3; every JWT cell remains future work. S1’s status-only response is historical, replaced by S2’s Basic entry point.

Run all tests over real embedded Tomcat with SpringBootTest(RANDOM_PORT) + RestAssured. Mode-specific contexts must be isolated. Status codes below assume the indicated identity is supplied with the authentication mechanism of that mode.

| Request | none | basic | jwt |
|---|---:|---:|---:|
| GET /api/public without credentials | 200 | 200 | 200 |
| GET /api/user without credentials | 200 | 401 | 401 |
| GET /api/admin without credentials | 200 | 401 | 401 |
| GET /api/user as USER | 200 | 200 | 200 |
| GET /api/admin as USER | 200 | 403 | 403 |
| GET /api/admin as ADMIN | 200 | 200 | 200 |
| GET /api/user with Basic credentials | 200 | 200 | 401 |
| GET /api/user with invalid Bearer JWT | 200* | 401* | 401 |
| POST /auth/login valid credentials | not present | not present | 200 + token |
| POST /auth/login invalid credentials | not present | not present | 401 |

`*` None ignores Authorization headers (invalid-bearer requests now verified for all three endpoints). In the completed Basic mode, bearer-only protected requests cannot authenticate and are expected to return 401; that exact scheme case is not yet tested in basic. Do not treat this table as a claim that arbitrary supplied invalid credentials are ignored on public routes.

Public-route invalid Basic credentials are now verified as 401 in P2.S2. Missing-versus-malformed JWT role handling remains a P3.S2/P4.S1–S2 contract decision. `/api/user` requires authentication, not ROLE_USER; an authenticated ADMIN does not need an implicit USER role to use it. No role hierarchy is implied.

Additional JWT tests: expired token, future not-before, wrong issuer, wrong audience, tampered signature, different signing key, unexpected algorithm, malformed token, missing roles, USER vs ADMIN role mapping. Use a controllable Clock or fixed test tokens, not Thread.sleep. Validate status, headers, response body, and observable trace when relevant.

## P1.S2 lifecycle evidence — executed

`mvn verify` passed on 2026-10-31: 7 HTTP cases plus 2 unit tests, zero failures/errors/skips (JDK 24, release 21). `RequestTraceIT.successfulRequestRecordsNestedOrder` runs for public/user/admin and checks 200, JSON, exact body, generated ID, and the exact six-event nested sequence. `concurrentRequestsKeepIndependentTraces` verifies distinct IDs and independent public/admin events plus an empty result for an unknown ID. Existing `PlainMvcIT` still passes all three endpoint tests.

`TraceEventSinkTest.completedTracesAreBoundedAndRequestStateIsRemoved` verifies eviction after 129 completions and active-attribute removal. `completedSnapshotCannotBeModifiedByReaders` verifies immutable snapshots. These unit tests supplement real HTTP; they do not replace Tomcat tests. See [actual traces and source map](learning/P1.S2-WALKTHROUGH.md). Error/async paths remain untested in this step.

## P1.S3 error-path evidence — executed

`mvn verify` passed on 2026-10-31: 9 integration cases + 2 unit tests, zero failures/errors/skips (JDK 24, release 21). New test-only fixtures observe REQUEST/ERROR separately; the executable jar contains neither the failing controller nor the probe. Default Boot error JSON contains status/error/original path and omits exception/trace/message fields in these tests.

| Test | Observed result |
|---|---|
| `ErrorPathIT.unmappedUrlReturns404ThroughResourceHandlerAndErrorDispatch` | GET `/api/not-mapped`: 404; resource-handler REQUEST, no REQUEST postHandle, completion exception null; Boot ERROR pre/post/completion |
| `ErrorPathIT.controllerExceptionReturns500AndNextRequestHasFreshTrace` | GET `/test/throw`: 500; controller event, no REQUEST postHandle, completion IllegalStateException; Boot ERROR processing; following GET public: fresh ID and normal success |

P1.S3 checkpoint inventory: `PlainMvcIT.publicEndpointReturnsPublicJson`, `userEndpointReturnsUserJsonWithoutCredentials`, `adminEndpointReturnsAdminJsonWithoutCredentials` (3); `RequestTraceIT.successfulRequestRecordsNestedOrder` for public/user/admin plus `concurrentRequestsKeepIndependentTraces` (4); the two `ErrorPathIT` methods above (2); `TraceEventSinkTest.completedTracesAreBoundedAndRequestStateIsRemoved` and `completedSnapshotCannotBeModifiedByReaders` (2 unit tests). Prior sections retain historical step results; their former “error paths untested” statements are superseded by this section. Async lifecycle tests remain out of scope. [Actual traces and code commentary](learning/P1.S3-WALKTHROUGH.md).

## P2.S1 — mode isolation and security infrastructure executed

P2.S1 checkpoint `mvn verify` (2026-10-31): 15 HTTP integration cases + 9 unit cases, zero failures/errors/skips, JDK 24/release 21. P1 historical statements about classpath absence are superseded: security libraries are now present, but none has no security chain/proxy bean or servlet registration.

| Test | Executed cases and assertions |
|---|---|
| `NoneSecurityIT.noneHasNoSecurityInfrastructureAndIgnoresAuthorization` | public/user/admin ×3 with invalid bearer header; 200 exact JSON; no security chain/proxy bean or proxy servlet registration |
| `BasicSecurityIT.publicRequestTraversesSecurityAndReachesController` | public 200 exact JSON, no session cookie, actual proxy invocation, normal MVC trace, configured security filter list |
| `BasicSecurityIT.unauthenticatedProtectedRequestReturns401BeforeMvc` | user/admin ×2; empty 401, no challenge/cookie, proxy invoked, no MVC/controller events |
| `SecurityModeConfigTest.missingModeDefaultsToNone` | 1 unit case: missing value accepted as none |
| `SecurityModeConfigTest.acceptsImplementedModes` | 2 unit cases: none/basic accepted |
| `SecurityModeConfigTest.rejectsUnsupportedOrCombinedModes` | 4 unit cases: jwt, typo, combined, blank rejected |

All 9 previous HTTP cases and 2 sink unit tests also passed. Basic S1 has no credential processing: successful authentication, invalid Basic credentials, challenge headers, ADMIN role policy, and authenticated context cleanup remain unexecuted future steps. The cross-mode matrix above describes the intended completed modes, not S1's partial Basic behavior. [Walkthrough with actual evidence](learning/P2.S1-WALKTHROUGH.md).

## P2.S2 — password authentication and principal evidence executed

Final `mvn verify` on 2026-10-31: 21 integration + 9 unit cases, zero failures/errors/skips, JDK 24/release 21. This section supersedes S1's no-Basic-filter/no-challenge/empty-body assertions and statements that password authentication is untested. None identity/encoder absence is now asserted as well as security registration absence.

| BasicSecurityIT method | Cases / evidence |
|---|---|
| `publicRequestTraversesSecurityAndReachesController` | 1: anonymous public 200; proxy invoked; actual Basic filter present; full REQUEST trace |
| `unauthenticatedProtectedRequestReturns401WithoutApplicationHandler` | 2: user/admin 401 JSON status + exact Basic challenge, no cookie; only REQUEST filter events |
| `validCredentialsExposeExpectedPrincipalAndAuthority` | 2: USER/ADMIN GET user 200 exact body; expected principal/role, erased credentials, stored BCrypt prefix, proxy invocation |
| `invalidBasicAuthenticationReturnsChallengeWithoutApplicationHandler` | 4: wrong password, unknown user, malformed Basic header, public invalid Basic; 401/challenge/no cookie, safe error fields, no REQUEST principal observation |

Remaining cases: NoneSecurityIT 3; RequestTraceIT 4; PlainMvcIT 3; ErrorPathIT 2; SecurityModeConfigTest 7 unit cases; TraceEventSinkTest 2 unit cases. Basic entry-point sendError now renders via permitted ERROR dispatch; “without application handler” does not mean absence of Boot's error controller. USER access to admin is not yet constrained by a role matcher. Authenticated isolation, ADMIN 403, and detailed security diagnostics remain P2.S3. [Walkthrough and actual output](learning/P2.S2-WALKTHROUGH.md).

## P2.S3 — role authorization, diagnostics, and isolation executed

`mvn verify` on 2026-10-31 passed 27 integration + 9 unit cases, zero failures/errors/skips (JDK 24/release 21). BasicSecurityIT now has 15 cases: its existing 9 authentication cases plus these 6. All 12 other none/P1 integration cases and 9 unit cases remain green.

| New BasicSecurityIT method | Observed result |
|---|---|
| `userCannotAccessAdminAndReceives403WithoutChallenge` | USER/admin 403 JSON, no challenge/cookie, authenticated→denied events, no original MVC handler |
| `adminCanAccessAdmin` | ADMIN/admin 200 exact JSON and correct identity |
| `authenticatedRequestDoesNotAuthenticateNextAnonymousRequest` | ADMIN/user 200 then anonymous/user 401; distinct IDs, no session dependency |
| `concurrentRequestsKeepAuthenticatedIdentitiesSeparate` | Concurrent USER/user and ADMIN/admin succeed with independent principals, authorities, IDs, and traces |
| `authenticatedMissingRoutePreserves404ThroughErrorDispatch` | Authenticated missing route remains 404 with original path after ERROR rendering, no challenge |
| `directErrorRequestRequiresAuthentication` | Initial anonymous `/error` returns 401/challenge despite internal ERROR permission |

Every Basic trace assertion also checks that the worker-thread context contains no authentication after the real security proxy returns. A defensive test clear runs only after recording the observation. Events distinguish early credential failure (challenge only), anonymous authorization failure, authenticated role denial, and allowed MVC execution. Production REQUEST traces still omit ERROR callbacks; no claims about full async timelines are made. Earlier step counts/policies remain historical, not the current contract.

The Basic bearer-only wrong-scheme cell remains a planned comparative case rather than a claimed executed result; all JWT cells remain planned. See [actual trace and test inventory](learning/P2.S3-WALKTHROUGH.md).

## P3.S1 — RSA/JWT foundation executed (unit checks, not JWT HTTP)

`mvn verify` on 2026-10-31 passed 27 existing HTTP integration cases + 13 unit cases, zero failures/errors/skips, JDK 24/release 21. New Surefire `RsaJwtTest` cases:

| Method | Direct cryptographic result |
|---|---|
| `developmentKeysAreRsa2048WithMatchingPublicAndPrivateModulus` | ≥2048-bit RSA public key and matching private/public modulus |
| `rs256TokenHasExpectedHeaderAndClaimsAndVerifiesWithPublicKey` | Three compact parts, RS256/JWT/kid header, seven expected claims, matching public-key verification true |
| `differentPublicKeyCannotVerifySignature` | Verification false with another generated public key |
| `changedPayloadCannotVerifyOriginalSignature` | Verification false after replacing payload without re-signing |

No new HTTP test or token-acceptance status is claimed. No current-time/issuer/audience validation or JWT SecurityContext population is implemented. Keys stay in test memory; complete tokens/private material are not logged. Existing suites retain their P2.S3 counts; the four crypto cases increase unit cases from 9 to 13. [Claim policy for future runtime enforcement](JWT-CLAIM-CONTRACT.md), [actual output and source map](learning/P3.S1-WALKTHROUGH.md).
