# P3 — RS256 JWT foundation, validation-first, then issuance

**Status:** Planned. P3.S1 is the first key/JWT step; P3.S2 introduces jwt runtime mode, and P3.S3 introduces login issuance.

**Objective:** learn RS256 public/private key separation and Nimbus-backed JWT processing. Use Spring Security OAuth2 Resource Server for JWT validation, **not** an OAuth2 Authorization Server or grant flow.

## P3.S1 — RSA key material and JWT anatomy
Create a safe local development RSA keypair (at least 2048-bit), loading private key only in the issuer component and public key in the decoder. Do not commit real/private secrets; prefer test-generated keys or local ignored files. Introduce `JwtClaimsSet` with sub, roles, iss, aud, iat, nbf (if used), exp; sign with `NimbusJwtEncoder` RS256 in tests. Test header, claims and verification using public key.
Before S2, document a claim contract: required claim presence and types, optional `iat`/`nbf` rules, intended issuer/audience, token lifetime, accepted clock skew, and deterministic time strategy. Separate checking a present timestamp from requiring that timestamp to exist. Define missing roles versus malformed roles without inventing extra identity features. Ignore local private-key files before generating them; the current generic `.gitignore` is not evidence that a newly chosen key path is excluded.
**Learning:** JWS vs JWT, RS256, private signing vs public verification, JWK, kid (if relevant), Base64URL not encryption.

## P3.S2 — Resource Server validates test-issued tokens
Add OAuth2 Resource Server starter and `jwt` mode. Configure `NimbusJwtDecoder` with RSA public key and RS256; validate issuer, audience and timestamps. Use tokens minted in integration tests, no login endpoint yet. Test valid USER/ADMIN bearer tokens and invalid signature/expired/issuer/audience tokens. Use a converter to map roles claim to ROLE_* authorities, reusing the endpoint policy established in P2. This is the minimum working conversion; P4 finalizes edge cases rather than rebuilding it. Extend the existing validated mode selector to accept `jwt` only here; retain explicit none isolation and reject unsupported values. Load the verification key locally: an issuer claim string is not a request to discover or contact an external identity provider. Basic authentication remains disabled on bearer-protected JWT routes.
**Learning:** BearerTokenAuthenticationFilter, JwtAuthenticationProvider, decoder, validators, JwtAuthenticationToken, SecurityContext.

## P3.S3 — Simple local login issuer
Add POST `/auth/login` with JSON username/password, validate via AuthenticationManager against in-memory users, issue RS256 JWT using `NimbusJwtEncoder`. Permit the local login endpoint only in jwt mode when introduced. Reuse the in-memory credential infrastructure deliberately without enabling Basic authentication on JWT-protected requests. Do not implement OAuth2 endpoints, grants, refresh tokens or consent. Test valid login returns token accepted by protected endpoint, bad credentials 401, no passwords/secrets in claims/logs.
**Learning:** initial credential authentication vs later stateless bearer verification.

## Deliverables and exit criteria
Deliver each P3.S1–S3 walkthrough with its own step, then audit them at phase completion; update FLOWS-COMPARED and TEST-MATRIX; `mvn verify` passing. Document clearly why issuer and resource server can coexist in one app yet remain logically separate.
