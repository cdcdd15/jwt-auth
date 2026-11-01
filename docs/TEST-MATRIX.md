# HTTP Integration Test Matrix — P1–P4 and P5.S1 verified; final audit planned

Current verified checkpoint: **P5.S1**, with 153 real HTTP integration cases and 29 unit cases passing on JDK 24/release 21. One shared 13-row comparison contract now runs in none/basic/jwt isolated contexts (39 cases). Mode-specific deeper coverage remains intact. P5.S2 final guide/audit remains planned. See [matrix, three diagrams and actual traces](learning/P5.S1-WALKTHROUGH.md).

## Shared completed-mode matrix — executed in P5.S1

The table describes the executed shared comparison contract, supplemented by mode-specific cases. P5.S1 runs all rows below in each mode. Basic S1’s status-only response remains historical, replaced by the completed challenge behavior. Login absence in basic is checked with valid Basic credentials so authorization does not hide the missing mapping.

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
| POST /auth/login valid credentials | 404 | 404 with valid Basic | 200 + token |
| POST /auth/login invalid credentials | 404 | 404 with valid Basic | 401 |
| GET /api/public with invalid Basic | 200 | 401 | 200 |
| GET /api/public with malformed Bearer | 200 | 200 | 401 |
| GET /api/not-mapped with native authentication | 404 | 404 | 404 |

`*` None ignores Authorization headers (invalid-bearer requests now verified for all three endpoints). Basic bearer-only protected requests cannot authenticate and now return 401 in the shared comparison. Do not treat this table as a claim that arbitrary supplied invalid credentials are ignored on public routes.

Public-route invalid Basic credentials are now verified as 401 in P2.S2. Missing/empty JWT roles now have P4.S1 HTTP evidence; malformed-role rejection now has P4.S2 HTTP evidence. `/api/user` requires authentication, not ROLE_USER; an authenticated ADMIN does not need an implicit USER role to use it. No role hierarchy is implied.

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

## P3.S2 — Resource Server JWT HTTP validation executed

Final `mvn verify` on 2026-10-31: 35 integration + 25 unit cases, zero failures/errors/skips, JDK 24/release 21. `JwtValidationIT` adds 8 real embedded-Tomcat cases; all prior 27 none/basic HTTP cases remain green. Test signing uses private keys only in memory; the runtime loads a temporary public PEM through its actual key configuration and validates time against a fixed test Clock.

| JwtValidationIT method | Cases and assertions |
|---|---|
| `publicEndpointDoesNotRequireToken` | 1: public 200 exact map/anonymous trace; live bearer chain without Basic; no JwtEncoder bean or login mapping |
| `validTokenReachesExpectedEndpoint` | 2: USER/user and ADMIN/admin 200, JSON exact map, no cookie, authenticated MVC trace |
| `invalidTokenReturns401BeforeApplicationHandler` | 4: different signing key, expired beyond skew, wrong issuer, wrong audience → 401/Bearer challenge/no cookie; only outer trace events |
| `basicCredentialsCannotAuthenticateJwtMode` | 1: Basic-only protected access → 401/Bearer challenge with anonymous downstream trace |

New unit checks: JwtClaimContractValidatorTest 8 (valid normalized map, five invalid shapes/values, missing/empty roles as candidates); JwtDecoderConfigTest 3 (missing/invalid/weak public key). Updated SecurityModeConfigTest 8 accepts jwt; existing RsaJwtTest 4 and TraceEventSinkTest 2 also pass, making 25 unit cases. These normalized-map examples are not the full raw malformed-claim matrix. Missing bearer, role-denial/no-role HTTP, future nbf, wrong algorithm, malformed token, timestamp boundaries, and detailed JWT cleanup diagnostics remain P4. Earlier “JWT unimplemented” statements are historical and superseded here. [Actual traces and complete commentary](learning/P3.S2-WALKTHROUGH.md).

## P3.S3 — local issuance executed

Verified 2026-11-01: `mvn verify`, BUILD SUCCESS, 14.459 seconds; 44 HTTP + 29 unit cases, zero failures/errors/skips. The prior S2 encoder/login absence assertions are replaced by presence assertions; its fixtures now provide both key paths.

| Test | Cases / observed result |
|---|---|
| `JwtLoginIT.loginIssuesSafeTokenAcceptedByProtectedEndpoint` | 2: USER/ADMIN login 200, no-store/no cookie, exact three response fields, seven safe claims, deterministic 300-second lifetime, trusted roles despite forged request roles; issued bearer gets user/admin 200 |
| `JwtLoginIT.badCredentialsReturnGeneric401WithoutToken` | 2: wrong password/unknown user → identical 401 JSON, no token/challenge/cookie, rejection trace |
| `JwtLoginIT.malformedLoginReturns400WithoutToken` | 3: missing username, empty password, invalid JSON → 400 without token or error-detail leaks |
| `NoneSecurityIT.loginEndpointIsAbsentInNoneMode` | 1: POST login → 404 |
| `BasicSecurityIT.loginEndpointIsAbsentInBasicMode` | 1: authenticated POST login → 404 |
| `JwtIssuerConfigTest` | 2 unit: mismatched pair and invalid PKCS8 rejected |
| `IssuerSecretHandlingTest` | 2 unit: record display redacted; temporary directory/files owner-only and removed |

HTTP suite totals: BasicSecurityIT 16, NoneSecurityIT 4, JwtValidationIT 8, JwtLoginIT 7, PlainMvcIT 3, RequestTraceIT 4, ErrorPathIT 2. Unit suite totals: SecurityModeConfigTest 8, TraceEventSinkTest 2, RsaJwtTest 4, JwtClaimContractValidatorTest 8, JwtDecoderConfigTest 3, JwtIssuerConfigTest 2, IssuerSecretHandlingTest 2. Historical inventories above remain checkpoint evidence rather than current counts.

## P4.S1 — conversion and authorization executed

Final `mvn verify` on 2026-11-01: BUILD SUCCESS in 15.272 seconds, 61 HTTP + 29 unit cases, zero failures/errors/skips (JDK 24/release 21). A first sandboxed run failed because Mockito could not self-attach; the final run used the required process permissions.

| New JwtValidationIT method | Cases / evidence |
|---|---|
| `validatedRolesBecomeRequestAuthorities` | 5: USER/ADMIN/OTHER → exact ROLE_* authority; missing/empty → none; actual request name equals subject |
| `endpointPolicyDistinguishesAuthenticationFromAdminRole` | 9: USER/user 200, USER/admin 403, ADMIN/user and admin 200, missing/empty user 200 and admin 403, OTHER/admin 403; exact allowed/denied trace and no cookies |
| `missingBearerReturns401BeforeMvc` | 2: user/admin 401 with plain Bearer challenge; no MVC events or session cookie |
| `bearerRequestDoesNotEstablishSessionAuthentication` | 1: ADMIN bearer succeeds; later request without bearer but with arbitrary JSESSIONID fails 401; no session cookies |

JwtValidationIT now has 25 cases, replacing its previous total of 8. All other S3 totals stay unchanged: BasicSecurityIT 16, NoneSecurityIT 4, JwtLoginIT 7, PlainMvcIT 3, RequestTraceIT 4, ErrorPathIT 2. Unit inventory remains 29. Test-only IdentityProbe is explicitly imported and excluded from component scanning. No production policy or custom filter is added. Missing/empty-role and role-denial statements in prior checkpoint sections are historical and superseded here; malformed roles, raw claims and timestamp boundaries remain P4.S2, expanded context diagnostics P4.S3.

## P4.S2 — raw negative validation and deterministic boundaries executed

Final `mvn verify` on 2026-11-01: BUILD SUCCESS, 21.343 seconds, 106 HTTP + 29 unit cases, zero failures/errors/skips (JDK 24/release 21). JwtNegativeValidationIT adds 45 cases; all prior 61 HTTP cases and 29 unit cases remain green. Initial execution found three accepted malformed representations (numeric sub, null iat, null nbf); StrictJwtDecoder closes those gaps after Nimbus verification and before authentication conversion.

| JwtNegativeValidationIT method | Cases / evidence |
|---|---|
| `invalidRawTokensFailClosedBeforeMvc` | 34: tampering, unknown key, HS256, malformed token; expired/future nbf/wrong issuer/audience; missing sub/iss/aud/exp; null sub/iss/aud/exp/iat/nbf/roles; numeric sub/iss/aud; textual exp/iat/nbf; boolean exp; blank sub; empty/mixed aud; scalar/object roles and numeric/null/blank role element. All 401 invalid_token, no cookie, outer-only trace |
| `timestampSkewHasDeterministicBoundaries` | 6: exp NOW−61 →401, NOW−60/−59 →200; nbf NOW+61 →401, NOW+60/+59 →200; status/challenge or exact JSON, no cookie, appropriate trace |
| `permittedRawRepresentationsRemainAuthenticated` | 5: single-string aud, missing iat/nbf/roles, empty roles →200 exact user JSON, no cookie, authenticated MVC trace |

HTTP suite totals: JwtNegativeValidationIT 45, JwtValidationIT 25, JwtLoginIT 7, BasicSecurityIT 16, NoneSecurityIT 4, PlainMvcIT 3, RequestTraceIT 4, ErrorPathIT 2. Unit totals remain the P3.S3 inventory of 29. P4.S3 still owns enhanced diagnostics/context-isolation instrumentation. Prior statements that raw malformed claims and boundaries were pending describe earlier checkpoints and are superseded here.

## P4.S3 — diagnostics and identity isolation executed

Final `mvn verify`, 2026-11-01: BUILD SUCCESS, 23.245 seconds; 114 HTTP + 29 unit cases, zero failures/errors/skips (JDK 24/release 21). JwtDiagnosticsIT adds eight cases; all prior 106 HTTP cases remain passing. Updated trace expectations add challenge to early/anonymous JWT rejection and denied to authenticated role failure; HTTP contracts remain unchanged.

| JwtDiagnosticsIT method | Cases / evidence |
|---|---|
| `diagnosticsDistinguishAuthenticationAndAuthorization` | 5: anonymous public 200, anonymous protected 401, malformed bearer 401, USER/admin 403, valid USER/user 200; configured order vs observed real filter entries; failure markers and MVC reachability; no cookie; cleared context |
| `authenticatedThenAnonymousRequestsRemainIndependent` | 1: ADMIN/user then anonymous/user; distinct IDs, trusted first identity, no later authenticated MVC identity, cleanup true |
| `concurrentBearerIdentitiesStayCorrelatedAndSeparate` | 1: concurrent USER/user and ADMIN/admin 200 with independent identity/authority/controller events and empty post-proxy contexts |
| `tracesAndCapturedOutputExcludeCredentialsAndKeyMaterial` | 1: bearer API request and real JSON login; sink and captured output contain neither complete bearer value, development password nor private-key PEM marker |

HTTP totals: JwtDiagnosticsIT 8, JwtNegativeValidationIT 45, JwtValidationIT 25, JwtLoginIT 7, BasicSecurityIT 16, NoneSecurityIT 4, PlainMvcIT 3, RequestTraceIT 4, ErrorPathIT 2. Unit inventory remains 29. Probe/decorator/spy configuration is test-only, not packaged application behavior. Callback traces remain REQUEST-only; no async or load-test claim. Historical pending-diagnostics and two-event JWT rejection excerpts above are superseded by this checkpoint.

## P5.S1 — shared comparative HTTP contract executed

Final `mvn verify`, 2026-11-01: BUILD SUCCESS in 29.739 seconds; 153 HTTP + 29 unit cases, zero failures/errors/skips (JDK 24/release 21). `AbstractSecurityComparison.equivalentRequestsExposeModeDifferences` runs 13 scenarios in each of NoneComparisonIT, BasicComparisonIT and JwtComparisonIT: 39 new cases. No production Java changes.

The shared rows are anonymous public/user/admin, native USER/user, USER/admin, ADMIN/admin, Basic/user, invalid-bearer/user, valid/invalid JSON credential login, public-invalid-Basic, public-invalid-bearer, and native-authenticated missing route. Each checks status/no cookie, appropriate body/challenge, correlation and callback invariants. None additionally asserts absent proxy and security events; secured modes assert one proxy. Basic login rows carry valid Basic authentication before asserting 404. Public invalid Basic gives none/basic/jwt 200/401/200; public invalid bearer gives 200/200/401. Missing authenticated routes give 404 in all three.

HTTP suite totals: NoneComparisonIT 13, BasicComparisonIT 13, JwtComparisonIT 13, JwtDiagnosticsIT 8, JwtNegativeValidationIT 45, JwtValidationIT 25, JwtLoginIT 7, BasicSecurityIT 16, NoneSecurityIT 4, PlainMvcIT 3, RequestTraceIT 4, ErrorPathIT 2. Unit inventory remains 29. Existing ErrorPathIT provides none-mode 404/500 dispatch evidence; comparison adds no synthetic secured-mode exception controller. [Current code map and diagrams](learning/P5.S1-WALKTHROUGH.md). Earlier untested wrong-scheme/comparison statements above are historical checkpoint notes, superseded here.
