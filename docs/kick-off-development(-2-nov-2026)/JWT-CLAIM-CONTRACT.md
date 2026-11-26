# Local JWT claim contract

This is the implemented application contract, not a claim that Nimbus defaults enforce every rule. [J2.S2](learning/J2.S2-WALKTHROUGH.md) records raw-token/key evidence; [J2.S1](learning/J2.S1-WALKTHROUGH.md) records login/authorization and [J2.S3](learning/J2.S3-WALKTHROUGH.md) records lifecycle diagnostics. Java 17 and unconditional JWT apply.

## Header and trusted keys

Accept RS256 only using a locally configured RSA public key of at least 2048 bits. The decoder independently restricts algorithms; an untrusted header cannot choose another algorithm or key source. The local issuer signs with the matching PKCS8 private key and emits `typ: JWT`, `kid: local-issuer`. Tests can use a different fixture kid. Kid identifies a key; it is not proof of trust, a discovery URL or a required local validation claim. No remote JWKS or key rotation is implemented. Both keys are required at startup because this application also issues tokens. Missing/malformed/weak/mismatched keys fail closed.

## Claims

| Claim | Acceptance and local issuance |
|---|---|
| `sub` | Required original nonblank JSON string. Issuer uses authenticated principal name |
| `iss` | Required string exactly `urn:spring-security-learning:local`. This identifier triggers no network discovery |
| `aud` | Required nonblank string or nonempty array of nonblank strings containing `spring-security-learning-api`. Additional audiences are permitted |
| `exp` | Required NumericDate accepted by Nimbus and normalized to Instant, with expiration checking |
| `iat` | Issuer emits now. Optional when accepted, but a present value must be numeric and parse successfully. Explicit null is invalid. No independent future-iat/maximum-age policy |
| `nbf` | Issuer emits now. Optional when accepted, but a present value must be numeric, parse successfully and pass not-before checking. Explicit null is invalid |
| `roles` | Issuer derives an array of unprefixed strings from authenticated ROLE_ authorities. Missing or empty array is valid with no authorities. Present null/scalar/non-array or null/non-string/blank array members fail authentication |

NumericDate represents seconds since the Unix epoch, not milliseconds. The normalized validator checks required string/Instant/list representations and required audience. JwtIssuerValidator checks issuer equality; JwtTimestampValidator checks time. StrictJwtDecoder first delegates to Nimbus for trusted verification/normalized validation, then inspects original payload types using Nimbus SignedJWT parsing before returning the normalized Jwt. It guards distinctions normalization can erase, such as numeric subject becoming a string or null dates disappearing. It is a decorator, not a replacement cryptographic parser. Missing iat remains allowed even if normalization synthesizes an iat from exp.

## Time and authorization

Issuer lifetime is 300 seconds, with clock truncated to whole seconds. Validation uses an injected Clock and explicit 60-second skew. Fixed-clock HTTP tests accept exp exactly NOW−60 seconds and nbf exactly NOW+60, rejecting NOW−61/NOW+61 respectively. No sleeps are used. Future descriptive iat before exp and omitted iat/nbf are accepted controls; the boundary policy is not a claim of unlimited parser acceptance for arbitrary numbers.

After validation, JwtAuthenticationConverter maps roles to ROLE_ authorities. Only ROLE_ADMIN satisfies admin. ADMIN has no implied USER hierarchy; `/api/user` requires authentication rather than ROLE_USER. Missing/empty/unknown well-formed roles allow user access but do not grant admin. Malformed roles cause 401 before conversion, rather than an authorization 403.

Signature integrity alone does not prove issuer/audience/time/schema acceptance, establish SecurityContext or grant endpoint access. Spring Security performs authentication/context establishment and subsequent authorization. Missing bearer on protected routes yields 401 with a Bearer challenge. Invalid bearer yields 401 even on public routes because authentication runs first. Valid non-ADMIN admin access yields 403. HTTP Basic cannot authenticate protected routes.

## Issuance and secrets

POST `/auth/login` authenticates local JSON credentials using DaoAuthenticationProvider and BCrypt, then signs trusted claims with NimbusJwtEncoder. Success returns accessToken/tokenType/expiresIn and no-store. Credential rejection is generic 401 JSON without a bearer challenge; malformed bodies are 400. Issuance does not install the credential result in the filter SecurityContext or create session authentication. No OAuth2 grant, authorization server or refresh endpoint exists.

JWT payloads are Base64URL encoded and readable, not encrypted. Do not include passwords/private keys in claims or print complete tokens. [Scope](IN-SCOPE-OUT-OF-SCOPE.md) bounds this local teaching application; [foundations](learning/JWT-FOUNDATIONS.md) explains the source pipeline.
