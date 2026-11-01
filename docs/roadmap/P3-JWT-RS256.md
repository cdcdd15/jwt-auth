# P3 — RS256 JWT foundation, validation-first, then issuance

**Status:** P3 complete. P3.S3 verified 2026-11-01: 44 HTTP integration + 29 unit cases passed in `mvn verify` (14.459 seconds, JDK 24/release 21). JWT startup now requires matching local public and private keys. P4 remains unimplemented.

**Objective:** learn RS256 public/private key separation and Nimbus-backed JWT processing. Use Spring Security OAuth2 Resource Server for JWT validation, **not** an OAuth2 Authorization Server or grant flow.

## P3.S1 — RSA key material and JWT anatomy

- [x] Verified 2026-10-31: `mvn verify` passed 27 HTTP integration + 13 unit cases, zero failures/errors/skips (JDK 24, release 21).
- `RsaKeyGenerator` creates fresh 2048-bit RSA pairs on demand; no Spring bean/startup hook or persisted key file. S1 uses the test-generated-key option: only the in-memory test issuer receives private material, while verification receives the public key.
- `RsaJwtTest` verifies key size/pair relationship, RS256 header and seven claims, matching-public-key verification, wrong-key failure, and changed-payload failure. No timestamp/issuer/audience HTTP validation is claimed.
- Test-scoped JOSE/Nimbus dependency only; executable runtime dependencies remain unchanged. `/.local-keys/` is ignored before any possible future export; no key directory/file was created.
- [Walkthrough with source commentary and actual output](../learning/P3.S1-WALKTHROUGH.md); [claim contract for S2/P4](../JWT-CLAIM-CONTRACT.md).

Create a safe local development RSA keypair (at least 2048-bit), loading private key only in the issuer component and public key in the decoder. Do not commit real/private secrets; prefer test-generated keys or local ignored files. Introduce `JwtClaimsSet` with sub, roles, iss, aud, iat, nbf (if used), exp; sign with `NimbusJwtEncoder` RS256 in tests. Test header, claims and verification using public key.
Before S2, document a claim contract: required claim presence and types, optional `iat`/`nbf` rules, intended issuer/audience, token lifetime, accepted clock skew, and deterministic time strategy. Separate checking a present timestamp from requiring that timestamp to exist. Define missing roles versus malformed roles without inventing extra identity features. Ignore local private-key files before generating them; the current generic `.gitignore` is not evidence that a newly chosen key path is excluded.
**Learning:** JWS vs JWT, RS256, private signing vs public verification, JWK, kid (if relevant), Base64URL not encryption.

## P3.S2 — Resource Server validates test-issued tokens

Historical checkpoint: S3 adds the runtime encoder/login and changes fixtures to owner-only temporary public/private files. S2’s absence assertions below describe its original checkpoint, not current behavior.

- [x] Verified 2026-10-31: `mvn verify` passed 35 HTTP integration + 25 unit cases, zero failures/errors/skips (JDK 24, release 21).
- Resource Server starter replaces the test-only JOSE declaration; none/basic regression suites still pass. Mode selector now accepts jwt; Basic filter is absent in that chain.
- Conditional decoder loads a local X.509 RSA public PEM via `app.jwt.public-key-path`, requires ≥2048 bits, uses RS256 only, and composes contract/issuer/timestamp validators with injected Clock and explicit 60-second skew.
- Real Tomcat `JwtValidationIT`: USER/user and ADMIN/admin 200; wrong signature, expired, issuer, and audience 401; anonymous public 200; Basic-only user access 401/Bearer challenge. No encoder bean or `/auth/login` mapping exists.
- Unit checks cover missing/malformed/weak public keys and representative normalized claim shapes. Full P4 wire-edge/role/time-boundary tests remain planned.
- [Walkthrough, live chain list, and actual correlated events](../learning/P3.S2-WALKTHROUGH.md). Test signing keys remain in memory; only a temporary public PEM is written/deleted.

Replace S1’s explicit test-scoped JOSE dependency with the OAuth2 Resource Server starter/runtime JOSE support (do not let the old test scope shadow transitive runtime dependencies), and add `jwt` mode. Configure `NimbusJwtDecoder` with RSA public key and RS256; implement the [claim contract](../JWT-CLAIM-CONTRACT.md), including required claim presence/types and issuer/audience/timestamps. Use a controlled clock and explicit 60-second skew; S1’s signature-only tests are not evidence of timestamp acceptance. Use tokens minted in integration tests, no login endpoint yet. Test valid USER/ADMIN bearer tokens and invalid signature/expired/issuer/audience tokens. Use a converter to map roles claim to ROLE_* authorities, reusing the endpoint policy established in P2. This is the minimum working conversion; P4 finalizes edge cases rather than rebuilding it. Extend the existing validated mode selector to accept `jwt` only here; retain explicit none isolation and reject unsupported values. Load the verification key locally: an issuer claim string is not a request to discover or contact an external identity provider. Basic authentication remains disabled on bearer-protected JWT routes.
**Learning:** BearerTokenAuthenticationFilter, JwtAuthenticationProvider, decoder, validators, JwtAuthenticationToken, SecurityContext.

## P3.S3 — Simple local login issuer

- [x] Shared in-memory DaoAuthenticationProvider, dedicated login ProviderManager, JSON controller, matching PKCS8 signing key and NimbusJwtEncoder implemented only for jwt.
- Valid USER/ADMIN login issues a 300-second RS256 token accepted over HTTP; wrong/unknown credentials return the same 401, malformed bodies return 400. None/basic have no login mapping.
- Four additional unit cases check key mismatch/format, redacted record display, and temporary-key permissions/removal. No P4 edge-case expansion.
- [Current source commentary, traces and test evidence](../learning/P3.S3-WALKTHROUGH.md).
Add POST `/auth/login` with JSON username/password, validate via AuthenticationManager against in-memory users, issue RS256 JWT using `NimbusJwtEncoder`. Permit the local login endpoint only in jwt mode when introduced. Reuse the in-memory credential infrastructure deliberately without enabling Basic authentication on JWT-protected requests. Do not implement OAuth2 endpoints, grants, refresh tokens or consent. Test valid login returns token accepted by protected endpoint, bad credentials 401, no passwords/secrets in claims/logs.
**Learning:** initial credential authentication vs later stateless bearer verification.

## Deliverables and exit criteria
Deliver each P3.S1–S3 walkthrough with its own step, then audit them at phase completion; update FLOWS-COMPARED and TEST-MATRIX; `mvn verify` passing. Document clearly why issuer and resource server can coexist in one app yet remain logically separate.
