# HTTP Integration Test Matrix — P1 and P2.S1 verified; credential/role/JWT rows planned

Current verified checkpoint: **P2.S1**, with 15 real HTTP integration cases and 9 unit cases passing on JDK 24 with release 21. `none` returns 200 for all three endpoints, including requests with an invalid bearer header, and has no security chain/proxy registration. Intermediate `basic` returns public 200 and protected 401 with an empty body, no challenge, and no session cookie. Users, successful Basic authentication, role authorization, and JWT remain planned. See [current evidence](learning/P2.S1-WALKTHROUGH.md).

## Target matrix for completed modes — not all cells executed

The table describes the completed project contract. Current P2.S1 covers the first three anonymous rows in none/basic and the invalid-bearer user request in none. Identity-based rows, challenge behavior, and every JWT cell require later steps. Basic S1's status-only entry point is temporary; it is not the final Basic challenge contract.

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

Before the assigned tests, record public-route invalid-credential handling in P2.S2/P2.S3, and missing-versus-malformed role handling in P3.S2/P4.S1–S2. `/api/user` requires authentication, not ROLE_USER; an authenticated ADMIN does not need an implicit USER role to use it. No role hierarchy is implied.

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

Latest `mvn verify` (2026-10-31): 15 HTTP integration cases + 9 unit cases, zero failures/errors/skips, JDK 24/release 21. P1 historical statements about classpath absence are superseded: security libraries are now present, but none has no security chain/proxy bean or servlet registration.

| Test | Executed cases and assertions |
|---|---|
| `NoneSecurityIT.noneHasNoSecurityInfrastructureAndIgnoresAuthorization` | public/user/admin ×3 with invalid bearer header; 200 exact JSON; no security chain/proxy bean or proxy servlet registration |
| `BasicSecurityIT.publicRequestTraversesSecurityAndReachesController` | public 200 exact JSON, no session cookie, actual proxy invocation, normal MVC trace, configured security filter list |
| `BasicSecurityIT.unauthenticatedProtectedRequestReturns401BeforeMvc` | user/admin ×2; empty 401, no challenge/cookie, proxy invoked, no MVC/controller events |
| `SecurityModeConfigTest.missingModeDefaultsToNone` | 1 unit case: missing value accepted as none |
| `SecurityModeConfigTest.acceptsImplementedModes` | 2 unit cases: none/basic accepted |
| `SecurityModeConfigTest.rejectsUnsupportedOrCombinedModes` | 4 unit cases: jwt, typo, combined, blank rejected |

All 9 previous HTTP cases and 2 sink unit tests also passed. Basic S1 has no credential processing: successful authentication, invalid Basic credentials, challenge headers, ADMIN role policy, and authenticated context cleanup remain unexecuted future steps. The cross-mode matrix above describes the intended completed modes, not S1's partial Basic behavior. [Walkthrough with actual evidence](learning/P2.S1-WALKTHROUGH.md).
