# Local JWT claim contract

**Status:** Defined in P3.S1; baseline runtime enforcement implemented in P3.S2. Real HTTP tests now cover valid USER/ADMIN tokens and signature/expiry/issuer/audience rejection. Unit tests cover representative normalized schema checks. P4 completes raw-token normalization, role-denial, malformed-claim, and time-boundary evidence. P3.S3 implements the local issuer: roles come only from authenticated authorities, iat/nbf use the injected clock truncated to seconds, exp is 300 seconds later, and the runtime header uses kid `local-issuer`. This document is an application contract, not a claim that Nimbus defaults enforce it automatically.

## Header and trust boundary

- Sign and accept RS256 only, using locally trusted RSA keys of at least 2048 bits. Configure the decoder's algorithm allowlist independently of the token's untrusted `alg` header.
- The issuer emits `typ: JWT`. The S1 fixture uses `kid: local-test-key` to select its single in-memory JWK. `kid` is an identifier, not proof of trust and not a URL. Key rotation, remote key discovery, and JWKS hosting remain out of scope.
- Private key material goes only to the signing side; verification receives only the public key. Do not serialize the private JWK into a token, response, trace, or committed file.

## Claims

| Claim | Issuer output / acceptance contract |
|---|---|
| `sub` | Required, nonblank string identifying the local principal; S1 example `user` |
| `iss` | Required string, exactly `urn:spring-security-learning:local`; this identifier does not trigger IdP discovery |
| `aud` | Required audience containing `spring-security-learning-api`; accept the standard single-string or array-of-strings JSON form and normalize appropriately |
| `exp` | Required valid NumericDate; reject expiry outside the configured clock-skew tolerance |
| `iat` | Issuer emits issuance NumericDate; optional on accepted tokens, but if present must have the correct type. It is descriptive issuance metadata, not a substitute for expiration or a separate maximum-token-age policy |
| `nbf` | Issuer emits issuance time; optional on accepted tokens. When present, require valid NumericDate and reject tokens too far in the future after applying clock skew |
| `roles` | Issuer emits a JSON array of unprefixed strings, e.g. `["USER"]`. Missing claim or an empty array means no role authorities. A present null, scalar, non-array, or array containing null/non-string/blank elements is malformed and must fail authentication |

NumericDate values represent seconds relative to the Unix epoch; Spring's decoded Java representation may be `Instant`. JSON parsing/type checks and application-required presence checks must be explicit. A timestamp validator checking a present value is not proof that it rejects a missing claim. The application validator checks normalized claim presence/types after Nimbus parsing and standard Spring conversion; it is not a raw JSON validator. P4 must verify raw malformed/null representations across that entire pipeline.

After signature/claims validation, map role names to `ROLE_` authorities. Unknown well-formed names confer no special USER/ADMIN rights; only `ROLE_ADMIN` satisfies admin access. There is no implied ADMIN→USER role hierarchy. An otherwise valid token without roles can authenticate for `/api/user` but must receive 403 on `/api/admin`. Malformed roles must produce 401 instead. The converter and authorization rules are present in S2, and missing/empty roles are accepted by the normalized validator unit checks; P4.S1 now verifies missing/empty-role HTTP outcomes: user 200, admin 403. Malformed-role HTTP cases remain P4.S2. A valid unknown role maps to its ROLE_ authority but grants no ADMIN permission. See [conversion and authorization evidence](learning/P4.S1-WALKTHROUGH.md).

## Time policy and test strategy

Local issuer lifetime: 300 seconds. Validation clock skew: 60 seconds. Use an injected/controlled validation clock for timestamp tests, with boundary cases in P4; do not sleep or depend on the machine's current date. The S1 fixture fixes issuance at `2030-01-01T12:00:00Z`, sets nbf to that instant and exp 300 seconds later. S1 deliberately checks signature and serialized claims only: the fixture's future date is not evidence that a runtime decoder would currently accept it.

## Authentication and authorization follow validation

A valid signature proves integrity against the trusted public key, not correct issuer/audience/time, authenticated context creation, or endpoint permission. Nimbus verification alone creates no Spring SecurityContext. P3.S2 now uses Resource Server's built-in BearerTokenAuthenticationFilter, JwtAuthenticationProvider, NimbusJwtDecoder, and a converter. P3.S3 now adds local credential-to-token issuance. See [the current walkthrough](learning/P3.S3-WALKTHROUGH.md). No OAuth2 grant/authorization server or external IdP is required.

Never include passwords, private keys, or other secrets in claims. Base64URL is readable encoding, not encryption. Token values must not appear in logs or learning traces. See [S1 signing evidence](learning/P3.S1-WALKTHROUGH.md) and [S2 runtime validation evidence](learning/P3.S2-WALKTHROUGH.md).
