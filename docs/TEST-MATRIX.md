# HTTP Integration Test Matrix — P1.S1–S2 verified; security rows planned

P1.S1 executed `PlainMvcIT.publicEndpointReturnsPublicJson`, `userEndpointReturnsUserJsonWithoutCredentials`, and `adminEndpointReturnsAdminJsonWithoutCredentials`: all passed, asserting 200, JSON content type, and exact parsed bodies (`message`: `public`, `user`, `admin`). `mvn verify` on 2026-10-31: 3 tests, 0 failures/errors/skips, JDK 24 with release 21. No Basic/JWT or Authorization-header cases have been executed. See [walkthrough and test-to-code map](learning/P1.S1-WALKTHROUGH.md).

The matrix below is the intended cross-mode contract; only its first three `none` cells are currently tested.

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

`*` Exact behavior for unrecognized Authorization schemes depends on mode configuration; test the intended contract and explicitly document the actual behavior. No-auth mode ignores Authorization headers. Basic mode should reject access without valid Basic authentication.

Additional JWT tests: expired token, future not-before, wrong issuer, wrong audience, tampered signature, different signing key, unexpected algorithm, malformed token, missing roles, USER vs ADMIN role mapping. Use a controllable Clock or fixed test tokens, not Thread.sleep. Validate status, headers, response body, and observable trace when relevant.

## P1.S2 lifecycle evidence — executed

`mvn verify` passed on 2026-10-31: 7 HTTP cases plus 2 unit tests, zero failures/errors/skips (JDK 24, release 21). `RequestTraceIT.successfulRequestRecordsNestedOrder` runs for public/user/admin and checks 200, JSON, exact body, generated ID, and the exact six-event nested sequence. `concurrentRequestsKeepIndependentTraces` verifies distinct IDs and independent public/admin events plus an empty result for an unknown ID. Existing `PlainMvcIT` still passes all three endpoint tests.

`TraceEventSinkTest.completedTracesAreBoundedAndRequestStateIsRemoved` verifies eviction after 129 completions and active-attribute removal. `completedSnapshotCannotBeModifiedByReaders` verifies immutable snapshots. These unit tests supplement real HTTP; they do not replace Tomcat tests. See [actual traces and source map](learning/P1.S2-WALKTHROUGH.md). Error/async paths remain untested in this step.
