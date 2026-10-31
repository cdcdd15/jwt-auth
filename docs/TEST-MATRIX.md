# HTTP Integration Test Matrix — P1.S1 baseline verified; remaining rows planned

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
