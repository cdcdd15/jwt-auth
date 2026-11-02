# Current JWT test matrix

J3.S2 verification on 2026-11-02: `mvn verify` using Eclipse Temurin 17.0.21+9 and Maven 3.9.12 passed **110 HTTP integration + 31 unit/context cases**, zero failures/errors/skips, **25.502 seconds**. This inventory is derived from that run's Surefire/Failsafe XML. Parameterized invocations count as cases, not distinct method declarations. Production and tests are unchanged by J3.S2.

All HTTP suites use `@SpringBootTest(RANDOM_PORT)`, embedded Tomcat and RestAssured. Test fixtures supply temporary matching keys and controlled clocks where needed; contexts are isolated where configuration differs. Surefire runs *Test. Failsafe integration-test and verify run *IT. `mvn package` alone does not exercise that gate.

## HTTP contracts and source reachability

| Request/input | Expected result | Pipeline/evidence owner |
|---|---|---|
| Anonymous GET public | 200 public JSON | Full security chain with anonymous identity, MVC; JwtAuthorizationIT |
| Valid JSON USER/ADMIN login | 200 accessToken/tokenType/expiresIn, no-store, trusted seven claims | AuthController → dedicated ProviderManager/Dao/BCrypt → JwtTokenService/Nimbus encoder; JwtLoginIT |
| Wrong/unknown login credentials | Same 401 invalid_credentials JSON, no token or bearer challenge | MVC local exception handler; JwtLoginIT |
| Malformed login shape | 400, no token | JSON conversion/manual required-value checks; JwtLoginIT |
| Valid USER or ADMIN bearer on user | 200 user JSON | Built-in bearer → provider → decoder/converter → authenticated rule → MVC; JwtAuthorizationIT |
| Valid USER bearer on admin | 403, insufficient permission | Authentication succeeds, ADMIN authorization fails; JwtAuthorizationIT/JwtDiagnosticsIT |
| Valid ADMIN bearer on admin | 200 admin JSON | ROLE_ADMIN permits controller; JwtAuthorizationIT |
| Missing bearer or Basic-only on protected route | 401 Bearer challenge, no application handler | Anonymous authorization translated to challenge; JwtAuthorizationIT |
| Missing/empty/unknown well-formed roles | user200, admin403 | Valid authentication, no ADMIN authority; JwtAuthorizationIT |
| Bad signature/key/algorithm/serialization/claims/raw types | 401 invalid_token, no MVC | Nimbus/semantic/strict raw checks; JwtNegativeValidationIT |
| Invalid bearer on public | 401, no public controller | Authentication failure precedes permitAll; JwtNegativeValidationIT |
| Exact ±60s/±61s boundaries | Defined accepted/rejected controls | Fixed clock and60s skew, no sleeps; JwtNegativeValidationIT |
| Completed allowed/denied/invalid then anonymous | Fresh401, separate IDs and cleared context | Real proxy finally observation; JwtDiagnosticsIT |
| Overlapping USER/ADMIN | Independent200/principals/authorities/workers | Two-party MVC barrier and bounded waits; JwtDiagnosticsIT |
| Authenticated missing resource/throwing fixture | Ordinary404/500 JSON, no challenge/cookie | REQUEST completion then test-observed Boot ERROR dispatch; ErrorPathIT |
| Anonymous error destinations/direct error path |401 before MVC, no dispatch probe | Security precedes application errors; ErrorPathIT |
| Successful public/user/admin | Exact eight-event nesting, generated UUID | Outer/security/interceptor/controller trace; RequestTraceIT |

The three-event invalid-bearer trace is entry/challenge/exit. Anonymous protected rejection adds anonymous and authorization.exit, and role denial records authenticated/denied/authorization.exit between outer markers. Standard successful REQUEST traces contain outer entry, security state, preHandle, controller, postHandle, afterCompletion, authorization.exit and outer exit. Exact lists and actual UUIDs live in [J2.S3](learning/J2.S3-WALKTHROUGH.md) and [J3.S1](learning/J3.S1-WALKTHROUGH.md).

## Executed suites and method inventory

### [ErrorPathIT](../src/test/java/com/example/securitylearning/ErrorPathIT.java) — 5 HTTP cases

- `anonymousRequestsFailSecurityBeforeApplicationErrors`
- `unmappedUrlReturns404ThroughResourceHandlerAndErrorDispatch`
- `controllerExceptionReturns500AndNextRequestHasFreshTrace`

### [JwtAuthorizationIT](../src/test/java/com/example/securitylearning/JwtAuthorizationIT.java) — 23 HTTP cases

- `publicEndpointDoesNotRequireToken`
- `missingBearerReturns401BeforeMvc`
- `basicCredentialsCannotAuthenticateProtectedEndpoint`
- `endpointPolicyDistinguishesAuthenticationFromAdminRole`
- `bearerRequestDoesNotEstablishSessionAuthentication`
- `validatedRolesBecomeRequestAuthorities`

### [JwtDiagnosticsIT](../src/test/java/com/example/securitylearning/JwtDiagnosticsIT.java) — 13 HTTP cases

- `tracesAndCapturedOutputExcludeCredentialsAndKeyMaterial`
- `concurrentBearerIdentitiesStayCorrelatedAndSeparate`
- `diagnosticsDistinguishAuthenticationAndAuthorization`
- `completedSecurityBranchDoesNotAuthenticateNextRequest`

### [JwtLoginIT](../src/test/java/com/example/securitylearning/JwtLoginIT.java) — 12 HTTP cases

- `badCredentialsReturnGeneric401WithoutToken`
- `loginIssuesTrustedTokenForEndpointPolicy`
- `malformedLoginReturns400WithoutToken`

### [JwtNegativeValidationIT](../src/test/java/com/example/securitylearning/JwtNegativeValidationIT.java) — 54 HTTP cases

- `permittedRawRepresentationsRemainAuthenticated`
- `invalidRawTokensFailClosedBeforeMvc`
- `timestampSkewHasDeterministicBoundaries`
- `invalidBearerFailsBeforePublicAuthorization`

### [RequestTraceIT](../src/test/java/com/example/securitylearning/RequestTraceIT.java) — 3 HTTP cases

- `successfulRequestRecordsNestedOrder`

### [JwtDecoderConfigTest](../src/test/java/com/example/securitylearning/config/JwtDecoderConfigTest.java) — 3 unit/context cases

- `invalidPublicKeyFails`
- `missingPublicKeyFailsInsteadOfGeneratingFallback`
- `undersizedRsaPublicKeyFails`

### [JwtIssuerConfigTest](../src/test/java/com/example/securitylearning/config/JwtIssuerConfigTest.java) — 2 unit/context cases

- `invalidPrivateKeyFails`
- `mismatchedSigningKeyFailsInsteadOfIssuingUnverifiableTokens`

### [JwtStartupTest](../src/test/java/com/example/securitylearning/config/JwtStartupTest.java) — 10 unit/context cases

- `unacceptableKeysFailContextStartup`
- `matchingDevelopmentKeysCreateOneEncoderAndDecoder`

### [IssuerSecretHandlingTest](../src/test/java/com/example/securitylearning/security/IssuerSecretHandlingTest.java) — 2 unit/context cases

- `temporaryKeysAreOwnerOnlyAndRemoved`
- `requestAndResponseDisplayAreRedacted`

### [JwtClaimContractValidatorTest](../src/test/java/com/example/securitylearning/security/JwtClaimContractValidatorTest.java) — 8 unit/context cases

- `validNormalizedClaimsSatisfyContract`
- `absentOrEmptyRolesRemainAnAuthenticationCandidate`
- `invalidClaimShapeOrRequiredValueFails`

### [RsaJwtTest](../src/test/java/com/example/securitylearning/security/RsaJwtTest.java) — 4 unit/context cases

- `changedPayloadCannotVerifyOriginalSignature`
- `developmentKeysAreRsa2048WithMatchingPublicAndPrivateModulus`
- `differentPublicKeyCannotVerifySignature`
- `rs256TokenHasExpectedHeaderAndClaimsAndVerifiesWithPublicKey`

### [TraceEventSinkTest](../src/test/java/com/example/securitylearning/tracing/TraceEventSinkTest.java) — 2 unit/context cases

- `completedTracesAreBoundedAndRequestStateIsRemoved`
- `completedSnapshotCannotBeModifiedByReaders`

## Evidence limits and remaining work

JwtStartupTest checks nine failing key configurations and one matching-pair context, supplementing direct parser tests. Unit checks also cover RSA integrity, normalized claim shapes, redacted object display, owner-only temporary-file cleanup, capacity128, active request cleanup and immutable snapshots. They supplement actual HTTP, not replace it.

Configured filters, real invocation decorators, REQUEST sink events and separate ERROR/context probes are different evidence layers. An observed filter entry does not prove successful authentication. Finally markers run on rejected paths. Cookie absence plus fresh-request/context assertions demonstrates the exercised stateless behavior, not impossibility of all servlet sessions or universal CSRF safety. Output checks cover tested branches and known secret values, not every possible future logging format.

Current behavior is verified. The [complete guide](learning/COMPLETE-GUIDE.md) contains the J3.S2 source-guide audit and all 21 HTTP method mappings; J3.S3 owns the final repository/history/package audit. Historical execution totals remain only in the roadmap and checkpoint walkthroughs, not current contract tables. See [claim contract](JWT-CLAIM-CONTRACT.md), [foundations](learning/JWT-FOUNDATIONS.md) and [scope](IN-SCOPE-OUT-OF-SCOPE.md).
